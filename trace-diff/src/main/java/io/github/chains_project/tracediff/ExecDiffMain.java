package io.github.chains_project.tracediff;

import picocli.CommandLine;
import io.github.chains_project.tracediff.statediff.StateDiffCommand;
import io.github.chains_project.tracediff.trace.ExecFreqDiffCommand;

@CommandLine.Command(
        name = Constants.EXEC_DIFF_COMMAND_NAME,
        mixinStandardHelpOptions = true,
        subcommands = {ExecFreqDiffCommand.class, StateDiffCommand.class},
        description =
                "The EXEC-DIFF command line tool for generating exec-frequency report and adding state diff info to it.",
        synopsisSubcommandLabel = "<COMMAND>")
public class ExecDiffMain {
    public static void main(String[] args) {
        // Make sure to run `sudo apt install chromium-chromedriver` before running the main jar.
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        new CommandLine(new ExecDiffMain()).execute(args);
    }
}
