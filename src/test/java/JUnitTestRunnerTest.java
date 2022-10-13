import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.kth.debug.JUnitTestRunner;
import se.kth.debug.Utility;

class JUnitTestRunnerTest {
    @TempDir private Path tempDir;
    private static final Path PATH_TO_JUNIT_LOGS =
            TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT_WITHOUT_DEBUG_INFO
                    .resolve("src")
                    .resolve("test")
                    .resolve("resources")
                    .resolve("junit-test-runner-logs");

    private String getActualLogs(String tests)
            throws IOException, InterruptedException, ClassNotFoundException {
        Path actualLog = Files.createFile(tempDir.resolve("log.txt"));
        String classpath =
                Utility.getClasspathForRunningJUnit(
                        TestHelper.getMavenClasspathFromBuildDirectory(
                                TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT_WITHOUT_DEBUG_INFO.resolve(
                                        "without-debug")));

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        "java", "-cp", classpath, JUnitTestRunner.class.getCanonicalName(), tests);
        processBuilder.redirectOutput(actualLog.toFile());

        Process p = processBuilder.start();
        p.waitFor(); // to ensure that logs are generated

        return Files.readString(actualLog);
    }

    @Test
    void main_canRunJunit4And5() throws IOException, InterruptedException, ClassNotFoundException {
        // assert
        String tests = "foo.junit.JUnit4Test foo.junit.JUnit5Test";
        Path expectedLogRegex = PATH_TO_JUNIT_LOGS.resolve("run-4-and-5.regex");

        assertThat(getActualLogs(tests), matchesPattern(Files.readString(expectedLogRegex)));
    }

    @Test
    void main_canRunTestMethodsAndClasses()
            throws IOException, InterruptedException, ClassNotFoundException {
        String tests = "foo.junit.JUnit4Test::test_concat foo.junit.JUnit5Test";
        Path expectedLogRegex = PATH_TO_JUNIT_LOGS.resolve("mix-of-method-and-class.regex");

        assertThat(getActualLogs(tests), matchesPattern(Files.readString(expectedLogRegex)));
    }

    @Test
    void main_cannotDiscoverAbstractTest()
            throws IOException, InterruptedException, ClassNotFoundException {
        // Read under 'Jupiter Concepts':
        // https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher
        String tests =
                "foo.junit.AbstractTest::test_junit4 foo.junit.AbstractTestForJUnit5::test_junit5";
        Path expectedLogRegex = PATH_TO_JUNIT_LOGS.resolve("abstract-test.regex");

        assertThat(getActualLogs(tests), matchesPattern(Files.readString(expectedLogRegex)));
    }

    @Test
    void main_canDiscoverTest_withoutItsNameStartingOrEndingInTest()
            throws IOException, InterruptedException, ClassNotFoundException {
        String tests = "foo.junit.IDoNotFollowConventions::sum";
        Path expectedLogRegex = PATH_TO_JUNIT_LOGS.resolve("disobey-conventions.regex");

        assertThat(getActualLogs(tests), matchesPattern(Files.readString(expectedLogRegex)));
    }
}
