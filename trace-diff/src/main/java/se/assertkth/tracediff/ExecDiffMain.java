package se.assertkth.tracediff;

import picocli.CommandLine;
import se.assertkth.tracediff.statediff.StateDiffCommand;
import se.assertkth.tracediff.trace.ExecFreqDiffCommand;

@CommandLine.Command(
        name = Constants.EXEC_DIFF_COMMAND_NAME,
        mixinStandardHelpOptions = true,
        subcommands = {ExecFreqDiffCommand.class, StateDiffCommand.class},
        description =
                "The EXEC-DIFF command line tool for generating exec-frequency report and adding state diff info to it.",
        synopsisSubcommandLabel = "<COMMAND>")
public class ExecDiffMain {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "/usr/share/chromedriver");
        new CommandLine(new ExecDiffMain()).execute(args);
    }
}
