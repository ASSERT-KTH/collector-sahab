package se.assertkth.mlf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.logging.Logger;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import se.assertkth.cs.commons.FileAndBreakpoint;
import se.assertkth.cs.commons.MethodForExitEvent;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.CtScanner;

public class MatchedLineFinder {

    private static final Logger LOGGER = Logger.getLogger("MLF");

    /**
     * Find the matched line between two Java source files.
     *
     * @param left previous version of source file
     * @param right revision of source file
     * @return matched line for left and matched line for right
     * @throws Exception raised from gumtree-spoon
     */
    public static Triple<String, String, String> invoke(File left, File right) throws IOException {
        Pair<Set<Integer>, Set<Integer>> diffLines = getDiffLines(left, right);

        Pair<CtTypeMember, CtTypeMember> methods = findMethods(left, right, diffLines);

        Set<Integer> matchedLinesLeft = getMatchedLines(diffLines.getLeft(), methods.getLeft());
        Set<Integer> matchedLinesRight = getMatchedLines(diffLines.getRight(), methods.getRight());
        String fullyQualifiedNameOfContainerClass =
                methods.getLeft().getParent(CtType.class).getQualifiedName();

        String breakpointsLeft = serialiseBreakpoints(fullyQualifiedNameOfContainerClass, matchedLinesLeft);
        String breakpointsRight = serialiseBreakpoints(fullyQualifiedNameOfContainerClass, matchedLinesRight);

        if (((CtExecutable<?>) methods.getLeft())
                .getSignature()
                .equals(((CtExecutable<?>) methods.getRight()).getSignature())) {
            // This file is particularly useful for patches where there are no matched lines, but we
            // need to record the return values.
            String serialisedMethods = serialiseMethods(
                    fullyQualifiedNameOfContainerClass, methods.getLeft().getSimpleName());
            return Triple.of(breakpointsLeft, serialisedMethods, breakpointsRight);
        }
        throw new RuntimeException(
                "Either the patch is changing the method signature or it could be a problem with GumTree mappings.");
    }

    private static Pair<Set<Integer>, Set<Integer>> getDiffLines(File left, File right) throws IOException {
        Set<Integer> src = new HashSet<>();
        Set<Integer> dst = new HashSet<>();

        Path script = Files.createTempFile(
                null, ".sh", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-rw-r--")));

        try (InputStream embeddedScript = MatchedLineFinder.class.getResourceAsStream("/diffn.sh")) {
            Files.copy(embeddedScript, script, StandardCopyOption.REPLACE_EXISTING);
        }

        String[] cmd = {"bash", script.toString(), "--no-index", left.getAbsolutePath(), right.getAbsolutePath()};

        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = pb.start();
        InputStreamReader isr = new InputStreamReader(p.getInputStream());
        BufferedReader rdr = new BufferedReader(isr);
        String line;
        while ((line = rdr.readLine()) != null) {
            String[] lineContents = line.split(":");
            if (lineContents[0].equals("Left")) {
                src.add(Integer.parseInt(lineContents[1]));
            }
            if (lineContents[0].equals("Right")) {
                dst.add(Integer.parseInt(lineContents[1]));
            }
        }
        if (src.size() == 0 && dst.size() == 0) {
            throw new NoDiffException("There is no diff between original and patched version");
        }
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
            statements.forEach(statement -> {
                if (!shouldBeIgnored(statement)
                        && !diffLines.contains(statement.getPosition().getLine())) {
                    lines.add(statement.getPosition().getLine());
                }
            });
            super.visitCtBlock(block);
        }

        /** Put breakpoints at variable read when it is part of a multi-variable declaration. */
        @Override
        public <T> void visitCtVariableRead(CtVariableRead<T> variableRead) {
            if (!diffLines.contains(variableRead.getPosition().getLine())) {
                lines.add(variableRead.getPosition().getLine());
            }
            super.visitCtVariableRead(variableRead);
        }

        /** Put breakpoints at variable write when it is part of a multi-variable declaration. */
        @Override
        public <T> void visitCtVariableWrite(CtVariableWrite<T> variableWrite) {
            if (!diffLines.contains(variableWrite.getPosition().getLine())) {
                lines.add(variableWrite.getPosition().getLine());
            }
            super.visitCtVariableWrite(variableWrite);
        }

        @Override
        public <S> void visitCtCase(CtCase<S> caseStatement) {
            List<CtStatement> caseBlock = caseStatement.getStatements();
            caseBlock.forEach(statement -> {
                if (!diffLines.contains(statement.getPosition().getLine()) && !shouldBeIgnored(statement)) {
                    lines.add(statement.getPosition().getLine());
                }
            });
            super.visitCtCase(caseStatement);
        }

        private static boolean shouldBeIgnored(CtElement element) {
            return element instanceof CtComment || element.isImplicit();
        }

        public Set<Integer> getLines() {
            return lines;
        }
    }

    private static Set<Integer> getMatchedLines(Set<Integer> diffLines, CtTypeMember method) {
        BlockFinder blockTraversal = new BlockFinder(diffLines);
        blockTraversal.scan(method);
        return blockTraversal.getLines();
    }

    private static Pair<CtTypeMember, CtTypeMember> findMethods(
            File left, File right, Pair<Set<Integer>, Set<Integer>> diffLines) throws IOException {
        // In an ideal case, srcNode of first root operation will give the method because APR
        // patches usually have
        // only one operation.
        // We also return the first method we find because we assume there will a patch inside only
        // one method.
        CtType<?> leftType = getType(left);
        CtType<?> rightType = getType(right);

        List<CtTypeMember> leftTypeMembers = getTypeMembers(leftType);
        List<CtTypeMember> rightTypeMembers = getTypeMembers(rightType);

        CtTypeMember leftDiffedTypeMember = findDiffedTypeMember(leftTypeMembers, diffLines.getLeft());
        CtTypeMember rightDiffedTypeMember = findDiffedTypeMember(rightTypeMembers, diffLines.getRight());

        if (leftDiffedTypeMember == null && rightDiffedTypeMember == null) {
            throw new RuntimeException("Neither left nor right diffed type member could be found.");
        }

        if (leftDiffedTypeMember == null) {
            leftDiffedTypeMember = findMapping(rightDiffedTypeMember, leftTypeMembers);
        }

        if (rightDiffedTypeMember == null) {
            rightDiffedTypeMember = findMapping(leftDiffedTypeMember, rightTypeMembers);
        }

        return Pair.of(leftDiffedTypeMember, rightDiffedTypeMember);
    }

    private static CtType<?> getType(File file) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(file.getAbsolutePath());
        CtModel model = launcher.buildModel();
        return model.getAllTypes().stream().findFirst().get();
    }

    private static List<CtTypeMember> getTypeMembers(CtType<?> type) {
        List<CtTypeMember> typeMembers = new ArrayList<>();
        for (CtTypeMember candidateTypeMember : type.getTypeMembers()) {
            if (candidateTypeMember instanceof CtMethod<?> || candidateTypeMember instanceof CtConstructor<?>) {
                typeMembers.add(candidateTypeMember);
            }
            if (candidateTypeMember instanceof CtClass<?>) {
                typeMembers.addAll(getTypeMembers((CtType<?>) candidateTypeMember));
            }
        }
        return typeMembers;
    }

    private static CtTypeMember findDiffedTypeMember(List<CtTypeMember> typeMembers, Set<Integer> diffLines) {
        Set<CtTypeMember> candidates = new HashSet<>();
        for (CtTypeMember typeMember : typeMembers) {
            if (typeMember.isImplicit()) {
                continue;
            }
            for (Integer position : diffLines) {
                if (typeMember.getPosition().getLine() < position
                        && position < typeMember.getPosition().getEndLine()) {
                    candidates.add(typeMember);
                }
            }
        }
        if (candidates.size() > 1) {
            throw new RuntimeException("More than 1 diffedTypeMember found");
        }
        if (candidates.size() == 0) {
            return null;
        }
        return candidates.stream().findFirst().get();
    }

    private static CtTypeMember findMapping(CtTypeMember whoseMapping, List<CtTypeMember> candidateMappings) {
        int expectedStartLine = whoseMapping.getPosition().getLine();
        for (CtTypeMember candidateMapping : candidateMappings) {
            if (isPlausibleMapping(whoseMapping, candidateMapping)) {
                return candidateMapping;
            }
        }
        throw new RuntimeException(
                "Could not find mapping for " + whoseMapping.getSimpleName() + " with start line " + expectedStartLine);
    }

    /** Compare method equality based on name, parameters, and return type. */
    private static boolean isPlausibleMapping(CtTypeMember first, CtTypeMember second) {
        if (first instanceof CtMethod && second instanceof CtMethod) {
            CtMethod<?> firstMethod = (CtMethod<?>) first;
            CtMethod<?> secondMethod = (CtMethod<?>) second;

            return firstMethod.getParameters().size()
                            == secondMethod.getParameters().size()
                    && firstMethod.getType().equals(secondMethod.getType())
                    && firstMethod.getSimpleName().equals(secondMethod.getSimpleName());
        }

        return false;
    }

    public static class NoDiffException extends RuntimeException {
        public NoDiffException(String message) {
            super(message);
        }
    }

    private static File prepareFileForGumtree(File cwd, String commit, File diffedFile, String revision)
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

    private static File copy(File cwd, File diffedFile, String revision) throws IOException, InterruptedException {
        final File revisionDirectory = new File(cwd.toURI().resolve(revision));
        revisionDirectory.mkdir();

        ProcessBuilder cpBuilder = new ProcessBuilder(
                "cp",
                diffedFile.getAbsolutePath(),
                revisionDirectory.toURI().resolve(diffedFile.getName()).getPath());
        cpBuilder.directory(cwd);
        Process p = cpBuilder.start();
        p.waitFor();

        return new File(revisionDirectory.toURI().resolve(diffedFile.getName()).getPath());
    }

    private static String serialiseBreakpoints(String fullyQualifiedClassName, Set<Integer> breakpoints)
            throws JsonProcessingException {
        // classfile convention is to use / instead of . for package names
        fullyQualifiedClassName = fullyQualifiedClassName.replace("/", ".");

        ObjectMapper mapper = new ObjectMapper();
        List<Integer> uniqueBreakpoints = new ArrayList<>(breakpoints);
        FileAndBreakpoint fileAndBreakpoint = new FileAndBreakpoint(fullyQualifiedClassName, uniqueBreakpoints);
        List<FileAndBreakpoint> fileAndBreakpoints = List.of(fileAndBreakpoint);

        return mapper.writeValueAsString(fileAndBreakpoints);
    }

    private static String serialiseMethods(String fullyQualifiedClassName, String methodName)
            throws JsonProcessingException {
        // classfile convention is to use / instead of . for package names
        fullyQualifiedClassName = fullyQualifiedClassName.replace("/", ".");

        ObjectMapper mapper = new ObjectMapper();
        MethodForExitEvent fileAndMethod = new MethodForExitEvent(methodName, fullyQualifiedClassName);
        List<MethodForExitEvent> fileAndMethods = List.of(fileAndMethod);

        return mapper.writeValueAsString(fileAndMethods);
    }

    private static void createInputFile(String content, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }
}
