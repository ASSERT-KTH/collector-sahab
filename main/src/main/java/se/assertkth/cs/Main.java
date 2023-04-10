package se.assertkth.cs;

import static se.assertkth.cs.preprocess.GitToLocal.getRevisions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import picocli.CommandLine;
import se.assertkth.cs.commons.CollectorAgentOptions;
import se.assertkth.cs.commons.Pair;
import se.assertkth.cs.commons.Revision;
import se.assertkth.cs.preprocess.PomTransformer;
import se.assertkth.mlf.MatchedLineFinder;
import se.assertkth.tracediff.Constants;
import se.assertkth.tracediff.ExecDiffMain;

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

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
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
        Path outputLeft = left.getPath().resolve("output.json");
        Path inputRight = Files.writeString(right.getPath().resolve("input.txt"), matchedLines.getRight());
        Path outputRight = right.getPath().resolve("output.json");
        Path methods = Files.writeString(left.getPath().resolve("methods.txt"), matchedLines.getMiddle());

        CollectorAgentOptions optionsLeft = new CollectorAgentOptions();
        optionsLeft.setClassesAndBreakpoints(inputLeft.toAbsolutePath().toFile());
        optionsLeft.setMethodsForExitEvent(methods.toAbsolutePath().toFile());
        optionsLeft.setExecutionDepth(executionDepth);
        optionsLeft.setOutput(outputLeft.toAbsolutePath().toFile());

        new PomTransformer(left, optionsLeft);

        CollectorAgentOptions optionsRight = new CollectorAgentOptions();
        optionsRight.setClassesAndBreakpoints(inputRight.toAbsolutePath().toFile());
        optionsRight.setMethodsForExitEvent(methods.toAbsolutePath().toFile());
        optionsRight.setExecutionDepth(executionDepth);
        optionsRight.setOutput(outputRight.toAbsolutePath().toFile());

        new PomTransformer(right, optionsRight);

        InvocationResult leftInvocation = mavenTestInvoker(left);
        if (leftInvocation.getExitCode() != 0) {
            throw new RuntimeException("Could not generate test data for left commit");
        }
        InvocationResult rightInvocation = mavenTestInvoker(right);
        if (rightInvocation.getExitCode() != 0) {
            throw new RuntimeException("Could not generate test data for right commit");
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
        String[] cmd = {
            "sdiff",
            Constants.ARG_SLUG,
            slug,
            Constants.ARG_COMMIT,
            right.getHash(),
            Constants.ARG_LEFT_REPORT_PATH,
            outputLeft.toAbsolutePath().toString(),
            Constants.ARG_RIGHT_REPORT_PATH,
            outputRight.toAbsolutePath().toString(),
            Constants.ARG_LEFT_SRC_PATH,
            left.resolveFilename(classfileName).toAbsolutePath().toString(),
            Constants.ARG_RIGHT_SRC_PATH,
            right.resolveFilename(classfileName).toAbsolutePath().toString(),
            // ghFullDiff is null
            Constants.ARG_SELECTED_TESTS,
            "", // empty test string
            Constants.ARG_TEST_LINK,
            "https://github.com/ASSERT-KTH",
            Constants.ARG_OUTPUT_PATH,
            outputPath,
        };
        ExecDiffMain.main(cmd);
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
