package se.kth.debug;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;

public class MatchedLineFinder {
    private static final Logger LOGGER = Logger.getLogger("MatchedLineFinder");

    public static void main(String[] args) throws Exception {
        File project = new File(args[0]);
        File diffedFile = getAbsolutePathWithGivenBase(project, args[1]);
        String left = args[2];
        String right = args[3];

        final String LEFT_FOLDER_NAME = "gumtree-left";
        final String RIGHT_FOLDER_NAME = "gumtree-right";

        File leftJava = prepareFileForGumtree(project, left, diffedFile, LEFT_FOLDER_NAME);
        File rightJava = prepareFileForGumtree(project, right, diffedFile, RIGHT_FOLDER_NAME);

        Diff diff = new AstComparator().compare(leftJava, rightJava);
        Set<Integer> diffLines = getDiffLines(diff.getRootOperations());

        CtMethod<?> method = findMethod(diff.getRootOperations());
        Set<Integer> matchedLines = getMatchedLines(diffLines, method);
        String fullyQualifiedNameOfContainerClass =
                method.getParent(CtClass.class).getQualifiedName();

        String output =
                String.format(
                        "%s=%s%n",
                        fullyQualifiedNameOfContainerClass, StringUtils.join(matchedLines, ","));
        try (FileWriter writer = new FileWriter("input.txt")) {
            writer.write(output);
        }
    }

    private static Set<Integer> getDiffLines(List<Operation> rootOperations) {
        Set<Integer> result = new HashSet<>();
        rootOperations.forEach(
                operation -> {
                    if (operation.getSrcNode() != null) {
                        result.add(operation.getSrcNode().getPosition().getLine());
                    }
                    if (operation.getDstNode() != null) {
                        result.add(operation.getSrcNode().getPosition().getLine());
                    }
                });
        return Collections.unmodifiableSet(result);
    }

    private static Set<Integer> getMatchedLines(Set<Integer> diffLines, CtMethod<?> method) {
        Set<Integer> result = new HashSet<>();
        List<CtStatement> statements = method.getBody().getStatements();
        statements.forEach(
                statement -> {
                    if (!diffLines.contains(statement.getPosition().getLine())) {
                        result.add(statement.getPosition().getLine());
                    }
                });
        return result;
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
        Optional<File> absolutePath =
                FileUtils.listFiles(base, new String[] {"java"}, true).stream()
                        .filter(file -> file.getName().equals(filename))
                        .findFirst();
        if (absolutePath.isEmpty()) {
            throw new RuntimeException(filename + " does not exist in " + base.getAbsolutePath());
        }
        return absolutePath.get();
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

    private static void writeToFile(String content, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
