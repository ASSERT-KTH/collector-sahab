package io.github.chains_project.cs;

import static io.github.chains_project.cs.preprocess.GitToLocal.getRevisions;

import io.github.chains_project.cs.commons.CollectorAgentOptions;
import io.github.chains_project.cs.commons.Pair;
import io.github.chains_project.cs.commons.Revision;
import io.github.chains_project.cs.preprocess.PomTransformer;
import io.github.chains_project.mlf.MatchedLineFinder;
import io.github.chains_project.tracediff.Constants;
import io.github.chains_project.tracediff.ExecDiffMain;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import picocli.CommandLine;

@CommandLine.Command(name = "collector-sahab", mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {
    @CommandLine.Option(
            names = {"-l", "--left"},
            description = "Left commit hash",
            required = true)
    private String leftHash;

    @CommandLine.Option(
            names = {Constants.ARG_SLUG},
            description = "The slug of the commit repo.",
            required = true)
    private String slug;

    @CommandLine.Option(
            names = {Constants.ARG_OUTPUT_PATH},
            description = "The path to the output file.",
            defaultValue = "output.html")
    String outputPath;

    @CommandLine.Option(
            names = {"-r", "--right"},
            description = "Right commit hash",
            required = true)
    private String rightHash;

    @CommandLine.Option(
            names = {"-p", "--project"},
            description = "Project directory",
            required = true)
    private Path projectDirectory;

    @CommandLine.Option(
            names = {"-c", "--classfile-name"},
            description = "Path to classfile",
            required = true)
    private String classfileName;

    @CommandLine.Option(
            names = {"--execution-depth"},
            description = "Execution depth",
            required = false)
    private int executionDepth = 0;

    @CommandLine.Option(
            names = {Constants.ARG_SELECTED_TESTS},
            description = "The selected tests",
            split = ",",
            required = false)
    // Runs all test by default
    private List<String> selectedTests = List.of();

    @CommandLine.Option(
            names = {Constants.ARG_EXCLUDE_RANDOM_VALUES},
            description = "Should random values be excluded?")
    private boolean excludeRandomValues;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Pair<Revision, Revision> revisions = getRevisions(projectDirectory, leftHash, rightHash);
        Revision left = revisions.getLeft();
        Revision right = revisions.getRight();

        Path leftClassfile = left.resolveFilename(classfileName);
        Path rightClassfile = right.resolveFilename(classfileName);

        Triple<String, String, String> matchedLines =
                MatchedLineFinder.invoke(leftClassfile.toFile(), rightClassfile.toFile());

        Path inputLeft = Files.writeString(left.getPath().resolve("input.txt"), matchedLines.getLeft());
        Path outputDirLeft = Files.createDirectories(left.getPath().resolve("output"));
        Path inputRight = Files.writeString(right.getPath().resolve("input.txt"), matchedLines.getRight());
        Path outputDirRight = Files.createDirectories(right.getPath().resolve("output"));
        Path methods = Files.writeString(left.getPath().resolve("methods.txt"), matchedLines.getMiddle());

        List<Path> pomFilesLeft = Files.walk(left.getPath())
                .filter(path -> path.getFileName().toString().equals("pom.xml"))
                .collect(Collectors.toList());
        // copy a backup of pom files
        for (Path pomFile : pomFilesLeft) {
            Files.copy(pomFile, pomFile.getParent().resolve("pom.xml.bak"));
        }

        List<Path> pomFilesRight = Files.walk(right.getPath())
                .filter(path -> path.getFileName().toString().equals("pom.xml"))
                .collect(Collectors.toList());
        // copy a backup of pom files
        for (Path pomFile : pomFilesRight) {
            Files.copy(pomFile, pomFile.getParent().resolve("pom.xml.bak"));
        }

        int repeats = excludeRandomValues ? Constants.REPEATS_FOR_RANDOM_EXCLUSION : 1;
        for (int i = repeats - 1; i >= 0; i--) {
            CollectorAgentOptions optionsLeft = new CollectorAgentOptions();
            optionsLeft.setClassesAndBreakpoints(inputLeft.toAbsolutePath().toFile());
            optionsLeft.setMethodsForExitEvent(methods.toAbsolutePath().toFile());
            optionsLeft.setExecutionDepth(executionDepth);
            optionsLeft.setOutput(outputDirLeft.resolve(i + ".json").toAbsolutePath().toFile());

            for (Path pomFile : pomFilesLeft) {
                // copy pom from backup
                Files.copy(pomFile.getParent().resolve("pom.xml.bak"), pomFile, StandardCopyOption.REPLACE_EXISTING);
                new PomTransformer(new Revision(pomFile.getParent(), leftHash), optionsLeft, selectedTests);
            }


            CollectorAgentOptions optionsRight = new CollectorAgentOptions();
            optionsRight.setClassesAndBreakpoints(inputRight.toAbsolutePath().toFile());
            optionsRight.setMethodsForExitEvent(methods.toAbsolutePath().toFile());
            optionsRight.setExecutionDepth(executionDepth);
            optionsRight.setOutput(outputDirRight.resolve(i + ".json").toAbsolutePath().toFile());

            for (Path pomFile : pomFilesRight) {
                Files.copy(pomFile.getParent().resolve("pom.xml.bak"), pomFile, StandardCopyOption.REPLACE_EXISTING);
                new PomTransformer(new Revision(pomFile.getParent(), rightHash), optionsRight, selectedTests);
            }

            mavenTestInvoker(left);
            mavenTestInvoker(right);
        }

        //        String slug,
        //        String commit,
        //        File leftReport,
        //        File rightReport,
        //        File srcFile,
        //        File dstFile,
        //        File ghFullDiff,
        //        String testsStr,
        //        String testLink,
        //        String outputPath,
        //        String allDiffsReportPath)
        List<String> cmd = new ArrayList<>(Arrays.asList(
            "sdiff",
            Constants.ARG_SLUG,
            slug,
            Constants.ARG_COMMIT,
            right.getHash(),
            Constants.ARG_LEFT_REPORT_PATH,
            outputDirLeft.toAbsolutePath().toString(),
            Constants.ARG_RIGHT_REPORT_PATH,
            outputDirRight.toAbsolutePath().toString(),
            Constants.ARG_LEFT_SRC_PATH,
            left.resolveFilename(classfileName).toAbsolutePath().toString(),
            Constants.ARG_RIGHT_SRC_PATH,
            right.resolveFilename(classfileName).toAbsolutePath().toString(),
            // ghFullDiff is null
            Constants.ARG_SELECTED_TESTS,
            String.join(",", selectedTests),
            Constants.ARG_TEST_LINK,
            "https://github.com/ASSERT-KTH",
            Constants.ARG_OUTPUT_PATH,
            outputPath));

        ExecDiffMain.main(cmd.toArray(new String[0]));
        return 0;
    }

    private static InvocationResult mavenTestInvoker(Revision project) throws IOException, MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(project.resolveFilename("pom.xml").toFile());
        request.setGoals(List.of("test"));
        request.setBatchMode(true);

        Invoker invoker = new DefaultInvoker();
        return invoker.execute(request);
    }
}
