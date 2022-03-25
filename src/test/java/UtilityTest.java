import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.kth.debug.Utility;

public class UtilityTest {
    @Test
    void getClasspathForRunningJUnit_providedClasspathIsAppendedToSystemClasspath(
            @TempDir Path tempDir) throws IOException {
        Path pathToDebuggerExecutable = Files.createFile(tempDir.resolve("debugger.jar"));
        Path pathToProjectClasspath = Files.createFile(tempDir.resolve("classpath.txt"));
        Path pathToProjectClasses = Files.createDirectories(tempDir.resolve("target/classes"));

        String actualClasspath =
                Utility.getClasspathForRunningJUnit(
                        new String[] {
                            pathToDebuggerExecutable.toString(),
                            pathToProjectClasspath.toString(),
                            pathToProjectClasses.toString()
                        });

        String expectedClasspath =
                String.format(
                        "%s%s%s%s%s",
                        pathToDebuggerExecutable.toAbsolutePath(),
                        File.pathSeparator,
                        pathToProjectClasspath.toAbsolutePath(),
                        File.pathSeparator,
                        pathToProjectClasses.toAbsolutePath());

        assertThat(
                Arrays.asList(actualClasspath.split(File.pathSeparator)),
                hasItems(expectedClasspath.split(File.pathSeparator)));
    }
}
