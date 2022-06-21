package se.kth.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.support.SpoonSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.CtScanner;

public class MatchedLineFinder {

    private static final Logger LOGGER = Logger.getLogger("MLF");

    public static void main(String[] args) throws Exception {
        File project = new File(args[0]);
        File diffedFile = resolveFilenameWithGivenBase(project, args[1]);
        String left = args[2];
        String right = args[3];

        final String LEFT_FOLDER_NAME = "gumtree-left";
        final String RIGHT_FOLDER_NAME = "gumtree-right";

        File leftJava = prepareFileForGumtree(project, left, diffedFile, LEFT_FOLDER_NAME);
        File rightJava = prepareFileForGumtree(project, right, diffedFile, RIGHT_FOLDER_NAME);

        Triple<String, String, String> output = invoke(leftJava, rightJava);

        createInputFile(output.getLeft(), "input-left.txt");
        createInputFile(output.getRight(), "input-right.txt");
        createInputFile(output.getMiddle(), "methods.json");
    }

    /**
     * Find the matched line between two Java source files.
     *
     * @param left previous version of source file
     * @param right revision of source file
     * @return matched line for left and matched line for right
     * @throws Exception raised from gumtree-spoon
     */
    public static Triple<String, String, String> invoke(File left, File right) throws Exception {
        Diff diff = new AstComparator().compare(left, right);
        Pair<Set<Integer>, Set<Integer>> diffLines = getDiffLines(diff);

        CtMethod<?> methodLeft = findMethod(diff);
        CtMethod<?> methodRight =
                (CtMethod<?>) new SpoonSupport().getMappedElement(diff, methodLeft, true);

        Set<Integer> matchedLinesLeft = getMatchedLines(diffLines.getLeft(), methodLeft);
        Set<Integer> matchedLinesRight = getMatchedLines(diffLines.getRight(), methodRight);
        String fullyQualifiedNameOfContainerClass =
                methodLeft.getParent(CtClass.class).getQualifiedName();

        String breakpointsLeft =
                serialiseBreakpoints(fullyQualifiedNameOfContainerClass, matchedLinesLeft);
        String breakpointsRight =
                serialiseBreakpoints(fullyQualifiedNameOfContainerClass, matchedLinesRight);

        if (methodLeft.getSignature().equals(methodRight.getSignature())) {
            // This file is particularly useful for patches where there are no matched lines, but we
            // need to record the return values.
            String methods =
                    serialiseMethods(
                            fullyQualifiedNameOfContainerClass, methodLeft.getSimpleName());
            return Triple.of(breakpointsLeft, methods, breakpointsRight);
        }
        throw new RuntimeException(
                "Either the patch is changing the method signature or it could be a problem with GumTree mappings.");
    }

    private static Pair<Set<Integer>, Set<Integer>> getDiffLines(Diff diff) {
        Set<Integer> src = new HashSet<>();
        Set<Integer> dst = new HashSet<>();
        diff.getAllOperations().forEach(
                operation -> {
                    if (operation.getSrcNode() != null) {
                        if (operation.getSrcNode().getPosition().isValidPosition()) {
                            // Nodes of insert operation should be inserted in dst
                            if (operation instanceof InsertOperation) {
                                dst.add(operation.getSrcNode().getPosition().getLine());
                                Set<Integer> x = linesAffectedInOtherTree(operation, diff, operation.getSrcNode().getPosition().getLine());
                                src.addAll(x);
                            } else {
                                src.add(operation.getSrcNode().getPosition().getLine());
                            }
                        }
                    }
                    if (operation.getDstNode() != null) {
                        if (operation.getDstNode().getPosition().isValidPosition()) {
                            dst.add(operation.getDstNode().getPosition().getLine());
                        }
                    }
                });
        return Pair.of(src, dst);
    }

    private static Set<Integer> linesAffectedInOtherTree(Operation operation, Diff diff, int lineNumber) {
        boolean isFromSource = operation instanceof DeleteOperation;
        CtElement srcNode = operation.getSrcNode();
        Set<Integer> lines = new HashSet<>();

        while (srcNode.getPosition().getLine() == lineNumber) {
            srcNode = srcNode.getParent();
            CtElement nodeInOtherTree =
                    new SpoonSupport()
                            .getMappedElement(
                                    diff,
                                    srcNode,
                                    isFromSource);
            int candidatePosition = nodeInOtherTree.getPosition().getLine();
            if (candidatePosition == lineNumber) {
                lines.add(candidatePosition);
            }
        }
        return lines;
    }

    static class BlockFinder extends CtScanner {
        private final Set<Integer> diffLines;
        private final Set<Integer> lines = new HashSet<>();

        private BlockFinder(Set<Integer> diffLines) {
            this.diffLines = diffLines;
        }

        /**
         * Get line numbers of statements, excluding diff lines, within a block.
         *
         * @param block element to be traversed
         * @param <R> return type of block, if any
         */
        @Override
        public <R> void visitCtBlock(CtBlock<R> block) {
            List<CtStatement> statements = block.getStatements();
            statements.forEach(
                    statement -> {
                        if (!diffLines.contains(statement.getPosition().getLine())) {
                            lines.add(statement.getPosition().getLine());
                        }
                    });
            super.visitCtBlock(block);
        }

        @Override
        public <S> void visitCtCase(CtCase<S> caseStatement) {
            List<CtStatement> caseBlock = caseStatement.getStatements();
            caseBlock.forEach(
                    statement -> {
                        if (!diffLines.contains(statement.getPosition().getLine())) {
                            lines.add(statement.getPosition().getLine());
                        }
                    });
            super.visitCtCase(caseStatement);
        }

        public Set<Integer> getLines() {
            return lines;
        }
    }

    private static Set<Integer> getMatchedLines(Set<Integer> diffLines, CtMethod<?> method) {
        BlockFinder blockTraversal = new BlockFinder(diffLines);
        blockTraversal.scan(method);
        return blockTraversal.getLines();
    }

    private static CtMethod<?> findMethod(Diff diff) {
        // In an ideal case, srcNode of first root operation will give the method because APR
        // patches usually have
        // only one operation.
        // We also return the first method we find because we assume there will a patch inside only
        // one method.
        for (Operation<?> operation : diff.getRootOperations()) {
            CtMethod<?> candidate;
            if (operation instanceof InsertOperation) {
                candidate =
                        (CtMethod<?>)
                                new SpoonSupport()
                                        .getMappedElement(
                                                diff,
                                                operation.getSrcNode().getParent(CtMethod.class),
                                                false);
            } else {
                candidate = operation.getSrcNode().getParent(CtMethod.class);
            }
            if (candidate == null) {
                LOGGER.warning(
                        operation.getSrcNode()
                                + ":"
                                + operation.getSrcNode().getPosition().getLine()
                                + " has no parent method.");
            } else {
                return candidate;
            }
        }
        throw new NoDiffException("No diff line is enclosed in method");
    }

    public static class NoDiffException extends RuntimeException {
        public NoDiffException(String message) {
            super(message);
        }
    }

    private static File resolveFilenameWithGivenBase(File base, String filename)
            throws FileNotFoundException {
        File absolutePath = Paths.get(base.getAbsolutePath()).resolve(filename).toFile();

        if (!absolutePath.exists()) {
            throw new FileNotFoundException(
                    filename + " does not exist in " + base.getAbsolutePath());
        }
        return absolutePath;
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

    private static String serialiseBreakpoints(
            String fullyQualifiedClassName, Set<Integer> breakpoints) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray array = new JsonArray();
        JsonObject object = new JsonObject();
        object.addProperty("fileName", fullyQualifiedClassName);
        object.add("breakpoints", gson.toJsonTree(breakpoints));
        array.add(object);

        return gson.toJson(array);
    }

    private static String serialiseMethods(String fullyQualifiedClassName, String methodName) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray array = new JsonArray();
        JsonObject object = new JsonObject();
        object.addProperty("className", fullyQualifiedClassName);
        object.addProperty("name", methodName);
        array.add(object);
        return gson.toJson(array);
    }

    private static void createInputFile(String content, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }
}
