package se.kth.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.support.SpoonSupport;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.CtScanner;

public class MatchedLineFinder {
    private static final Logger LOGGER = Logger.getLogger("MatchedLineFinder");

    public static void main(String[] args) throws Exception {
        cleanupPreviousExecutionFiles();

        File project = new File(args[0]);
        File diffedFile = getAbsolutePathWithGivenBase(project, args[1]);
        String left = args[2];
        String right = args[3];

        final String LEFT_FOLDER_NAME = "gumtree-left";
        final String RIGHT_FOLDER_NAME = "gumtree-right";

        File leftJava = prepareFileForGumtree(project, left, diffedFile, LEFT_FOLDER_NAME);
        File rightJava = prepareFileForGumtree(project, right, diffedFile, RIGHT_FOLDER_NAME);

        Triple<String, String, String> output = invoke(leftJava, rightJava);

        createInputFile(output.getLeft(), "input-left.txt");
        createInputFile(output.getMiddle(), "method-name.txt");
        createInputFile(output.getRight(), "input-right.txt");
    }

    /**
     * Find the matched line and the patched method between two Java source files.
     *
     * @param left previous version of source file
     * @param right revision of source file
     * @return matched line for left, patched method, matched line for right
     * @throws Exception raised from gumtree-spoon
     */
    public static Triple<String, String, String> invoke(File left, File right) throws Exception {
        Diff diff = new AstComparator().compare(left, right);
        Pair<Set<Integer>, Set<Integer>> diffLines = getDiffLines(diff.getRootOperations());

        CtMethod<?> methodLeft = findMethod(diff.getRootOperations());
        CtMethod<?> methodRight =
                (CtMethod<?>) new SpoonSupport().getMappedElement(diff, methodLeft, true);

        Set<Integer> matchedLinesLeft = getMatchedLines(diffLines.getLeft(), methodLeft);
        Set<Integer> matchedLinesRight = getMatchedLines(diffLines.getRight(), methodRight);
        String fullyQualifiedNameOfContainerClass =
                methodLeft.getParent(CtClass.class).getQualifiedName();

        String breakpointsLeft =
                serialiseBreakpoints(fullyQualifiedNameOfContainerClass, matchedLinesLeft);
        String methodName = serialiseMethodName(methodLeft);
        String breakpointsRight =
                serialiseBreakpoints(fullyQualifiedNameOfContainerClass, matchedLinesRight);

        return Triple.of(breakpointsLeft, methodName, breakpointsRight);
    }

    private static Pair<Set<Integer>, Set<Integer>> getDiffLines(List<Operation> rootOperations) {
        Set<Integer> src = new HashSet<>();
        Set<Integer> dst = new HashSet<>();
        rootOperations.forEach(
                operation -> {
                    if (operation.getSrcNode() != null) {
                        if (operation.getSrcNode().getPosition().isValidPosition()) {
                            // Nodes of insert operation should be inserted in dst
                            if (operation instanceof InsertOperation) {
                                dst.add(operation.getSrcNode().getPosition().getLine());
                            } else {
                                src.add(operation.getSrcNode().getPosition().getLine());
                            }
                        }
                    }
                    if (operation.getDstNode() != null) {
                        if (operation.getDstNode().getPosition().isValidPosition()) {
                            dst.add(operation.getSrcNode().getPosition().getLine());
                        }
                    }
                });
        return Pair.of(src, dst);
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
                        if (!diffLines.contains(statement.getPosition().getLine())
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
        BlockFinder blockTraversal = new BlockFinder(diffLines);
        blockTraversal.scan(method);
        return blockTraversal.getLines();
    }

    private static CtMethod<?> findMethod(List<Operation> rootOperations) {
        // In an ideal case, srcNode of first root operation will give the method because APR
        // patches usually have
        // only one operation.
        // We also return the first method we find because we assume there will a patch inside only
        // one method.
        for (Operation<?> operation : rootOperations) {
            CtMethod<?> candidate = operation.getSrcNode().getParent(CtMethod.class);
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
        throw new RuntimeException("No diff line is enclosed in method");
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

    private static String serialiseMethodName(CtMethod<?> method) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject object = new JsonObject();
        object.addProperty("name", method.getSimpleName());
        object.addProperty("className", method.getDeclaringType().getQualifiedName());

        return gson.toJson(object);
    }

    private static void createInputFile(String content, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }

    private static void cleanupPreviousExecutionFiles() {
        // No need to clear the following files since `scripts/compile_target.py` does that
        // 1. breakpoint input
        // 2. build files of project
        // GumTree files are assumed to be replaced since I have confidence on GumTree
        Path oldMethodName = new File("method-name.txt").toPath();
        try {
            Files.delete(oldMethodName);
        } catch (IOException e) {
            LOGGER.info("Could not delete method-name since it does not exist");
        }
    }
}
