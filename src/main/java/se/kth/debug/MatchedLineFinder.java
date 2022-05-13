package se.kth.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import se.kth.debug.struct.DebugeeType;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.CtScanner;

public class MatchedLineFinder {
    public static void main(String[] args) throws Exception {
        File project = new File(args[0]);
        File diffedFile = getAbsolutePathWithGivenBase(project, args[1]);
        String left = args[2];
        String right = args[3];

        final String LEFT_FOLDER_NAME = "gumtree-left";
        final String RIGHT_FOLDER_NAME = "gumtree-right";

        File leftJava = prepareFileForGumtree(project, left, diffedFile, LEFT_FOLDER_NAME);
        File rightJava = prepareFileForGumtree(project, right, diffedFile, RIGHT_FOLDER_NAME);

        Pair<String, String> output = invoke(leftJava, rightJava);

        createInputFile(output.getLeft(), "input-left.txt");
        createInputFile(output.getRight(), "input-right.txt");
    }

    /**
     * Find the matched line between two Java source files.
     *
     * @param left previous version of source file
     * @param right revision of source file
     * @return matched lines for left and matched lines for right
     * @throws Exception raised from gumtree-spoon
     */
    public static Pair<String, String> invoke(File left, File right) throws Exception {
        Diff diff = new AstComparator().compare(left, right);
        if (diff.getRootOperations().isEmpty()) {
            throw new NoDiffException("No diff line is enclosed in method");
        }
        Pair<Set<DebugeeType>, Set<DebugeeType>> store =
                initialiseStoreWithDiffLines(diff.getRootOperations());

        computeMatchedLines(store.getLeft());
        computeMatchedLines(store.getRight());

        String breakpointsLeft = serialiseBreakpoints(store.getLeft());
        String breakpointsRight = serialiseBreakpoints(store.getRight());

        return Pair.of(breakpointsLeft, breakpointsRight);
    }

    /**
     * Finds the enclosing methods of all the types in the store and computes breakpoints for the
     * method which encloses the diff line.
     *
     * @param debugeeTypes the types that need to be debugged
     */
    private static void computeMatchedLines(Set<DebugeeType> debugeeTypes) {
        for (DebugeeType store : debugeeTypes) {
            Set<CtMethod<?>> methods = store.getType().getMethods();
            for (CtMethod<?> method : methods) {
                int start = method.getPosition().getLine();
                int end = method.getPosition().getEndLine();

                if (store.getDiffLines().isEmpty()
                        || start <= Collections.min(store.getDiffLines())
                                && end >= Collections.max(store.getDiffLines())) {
                    store.getMatchedLines().addAll(getMatchedLines(store.getDiffLines(), method));
                }
            }
        }
    }

    private static Pair<Set<DebugeeType>, Set<DebugeeType>> initialiseStoreWithDiffLines(
            List<Operation> rootOperations) {
        Set<DebugeeType> src = new HashSet<>();
        Set<DebugeeType> dst = new HashSet<>();
        rootOperations.forEach(
                operation -> {
                    if (operation.getSrcNode() != null) {
                        if (operation.getSrcNode().getPosition().isValidPosition()) {
                            CtElement srcNode = operation.getSrcNode();
                            CtType<?> diffedType = srcNode.getParent(CtType.class);
                            // Nodes of insert operation should be inserted in dst
                            if (operation instanceof InsertOperation) {
                                appendOrCreateTypeAndBreakpointInStore(dst, diffedType, srcNode);
                            } else {
                                appendOrCreateTypeAndBreakpointInStore(src, diffedType, srcNode);
                            }
                        }
                    }
                    if (operation.getDstNode() != null) {
                        if (operation.getDstNode().getPosition().isValidPosition()) {
                            CtElement dstNode = operation.getDstNode();
                            CtType<?> diffedType = dstNode.getParent(CtType.class);
                            appendOrCreateTypeAndBreakpointInStore(dst, diffedType, dstNode);
                        }
                    }
                });
        return Pair.of(src, dst);
    }

    /**
     * Registers type in the debuggee type store. If the type already exists, the breakpoint is
     * added to the type.
     *
     * <p>If the diffed type is an anonymous class, outer classes are also registered.
     */
    private static void appendOrCreateTypeAndBreakpointInStore(
            Set<DebugeeType> revision, CtType<?> diffedType, CtElement diffedNode) {
        Optional<DebugeeType> candidate =
                revision.stream().filter(m -> m.getType().equals(diffedType)).findAny();
        if (candidate.isPresent()) {
            candidate.get().getDiffLines().add(diffedNode.getPosition().getLine());
        } else {
            DebugeeType store = new DebugeeType(diffedType);
            store.getDiffLines().add(diffedNode.getPosition().getLine());
            revision.add(store);
        }
        CtType<?> couldBeAnonymous = diffedType;
        if (couldBeAnonymous.isAnonymous()) {
            while (!couldBeAnonymous.isTopLevel()) {
                couldBeAnonymous = couldBeAnonymous.getParent(CtType.class);
                revision.add(new DebugeeType(couldBeAnonymous));
            }
        }
    }

    static class BlockFinder extends CtScanner {
        private final Set<Integer> diffLines;
        // We do not want to go in other classes because they are registered in store separately, so
        // they will be iterated upon in the future.
        private final CtType<?> currentType;
        private final Set<Integer> lines = new HashSet<>();

        private BlockFinder(Set<Integer> diffLines, CtType<?> currentType) {
            this.diffLines = diffLines;
            this.currentType = currentType;
        }

        /**
         * Get line numbers of statements, excluding diff lines, within a block.
         *
         * @param block element to be traversed
         * @param <R> return type of block, if any
         */
        @Override
        public <R> void visitCtBlock(CtBlock<R> block) {
            if (!block.getParent(CtType.class).equals(currentType)) {
                return;
            }
            List<CtStatement> statements = block.getStatements();
            statements.forEach(
                    statement -> {
                        if (!statement.isImplicit()
                                && !diffLines.contains(statement.getPosition().getLine())
                                && !isStatementPartOfDiffedBlock(statement)) {
                            lines.add(statement.getPosition().getLine());
                        }
                    });
            super.visitCtBlock(block);
        }

        private boolean isStatementPartOfDiffedBlock(CtStatement statement) {
            return diffLines.contains(statement.getParent(CtBlock.class).getPosition().getLine());
        }

        public Set<Integer> getLines() {
            return lines;
        }
    }

    private static Set<Integer> getMatchedLines(Set<Integer> diffLines, CtMethod<?> method) {
        BlockFinder blockTraversal = new BlockFinder(diffLines, method.getDeclaringType());
        blockTraversal.scan(method);
        return blockTraversal.getLines();
    }

    public static class NoDiffException extends RuntimeException {
        public NoDiffException(String message) {
            super(message);
        }
    }

    private static File getAbsolutePathWithGivenBase(File base, String filename) {
        List<File> absolutePath =
                FileUtils.listFiles(new File(base, "src"), new String[] {"java"}, true).stream()
                        .filter(file -> file.getName().equals(filename))
                        .collect(Collectors.toList());

        if (absolutePath.isEmpty()) {
            throw new RuntimeException(filename + " does not exist in " + base.getAbsolutePath());
        }
        if (absolutePath.size() > 1) {
            throw new RuntimeException("Use fully qualified names");
        }
        return absolutePath.get(0);
    }

    private static File prepareFileForGumtree(
            File cwd, String commit, File diffedFile, String revision)
            throws IOException, InterruptedException {
        if (checkout(cwd, commit) == 0) {
            return copy(cwd, diffedFile, revision);
        }
        throw new RuntimeException("Error occurred in checking out.");
    }

    private static int checkout(File cwd, String commit) throws IOException, InterruptedException {
        ProcessBuilder checkoutBuilder = new ProcessBuilder("git", "checkout", commit);
        checkoutBuilder.directory(cwd);
        Process p = checkoutBuilder.start();
        return p.waitFor();
    }

    private static File copy(File cwd, File diffedFile, String revision)
            throws IOException, InterruptedException {
        final File revisionDirectory = new File(cwd.toURI().resolve(revision));
        revisionDirectory.mkdir();

        ProcessBuilder cpBuilder =
                new ProcessBuilder(
                        "cp",
                        diffedFile.getAbsolutePath(),
                        revisionDirectory.toURI().resolve(diffedFile.getName()).getPath());
        cpBuilder.directory(cwd);
        Process p = cpBuilder.start();
        p.waitFor();

        return new File(revisionDirectory.toURI().resolve(diffedFile.getName()).getPath());
    }

    private static String serialiseBreakpoints(Set<DebugeeType> debugeeTypes) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray array = new JsonArray();
        for (DebugeeType store : debugeeTypes) {
            JsonObject object = new JsonObject();
            object.addProperty("fileName", store.getType().getQualifiedName());
            object.add("breakpoints", gson.toJsonTree(store.getMatchedLines()));
            array.add(object);
        }

        return gson.toJson(array);
    }

    private static void createInputFile(String content, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }
}
