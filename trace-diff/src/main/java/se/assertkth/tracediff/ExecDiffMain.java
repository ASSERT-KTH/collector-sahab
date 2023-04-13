package se.assertkth.tracediff;

import java.nio.file.Files;
import java.nio.file.Path;
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
        if (Files.exists(Path.of("/usr/share/chromedriver"))) {
            System.setProperty("webdriver.chrome.driver", "/usr/share/chromedriver");
        } else {
            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        }
        new CommandLine(new ExecDiffMain()).execute(args);
    }
}
