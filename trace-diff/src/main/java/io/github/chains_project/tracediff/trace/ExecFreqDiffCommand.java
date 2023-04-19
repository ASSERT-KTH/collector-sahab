package io.github.chains_project.tracediff.trace;

import io.github.chains_project.tracediff.Constants;
import io.github.chains_project.tracediff.sharedutils.GHHelper;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
        name = Constants.EXEC_FREQ_DIFF_COMMAND_NAME,
        mixinStandardHelpOptions = true,
        description = "Generate execution frequency report for a given commit.")
public class ExecFreqDiffCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = {Constants.ARG_SLUG},
            description = "The slug of the commit repo.")
    String slug;

    @CommandLine.Option(
            names = {Constants.ARG_COMMIT},
            description = "The ID of the commit.")
    String commit;

    @CommandLine.Option(
            names = {Constants.ARG_ORIGINAL_PATH},
            description = "The path to the original source directory.")
    File originalDir;

    @CommandLine.Option(
            names = {Constants.ARG_PATCHED_PATH},
            description = "The path to the patched source directory.")
    File patchedDir;

    @CommandLine.Option(
            names = {Constants.ARG_OUTPUT_PATH},
            description = "The path to the output source directory.")
    File outputDir;

    @CommandLine.Option(
            names = {Constants.ARG_FULL_REPORT_LINK},
            description = "The link to the full report.")
    String fullReportLink;

    @CommandLine.Option(
            names = {Constants.ARG_CHANGED_FILE_PATH},
            description = "The path to the changed file.")
    String changedFilePath;

    @CommandLine.Option(
            names = {Constants.ARG_SELECTED_TESTS},
            description = "The selected test(s) name(s).")
    String selectedTests;

    @Override
    public Integer call() throws Exception {
        new TraceAnalyzer()
                .generateTraceDiffsForGHChange(
                        slug,
                        commit,
                        originalDir,
                        patchedDir,
                        outputDir,
                        fullReportLink,
                        Arrays.asList(new String[] {changedFilePath}),
                        GHHelper.ChangeType.COMMIT,
                        selectedTests);
        return 0;
    }
}
