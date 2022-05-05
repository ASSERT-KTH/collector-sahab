import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.security.Permission;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.kth.debug.Collector;

public class CollectorTest {
    /** Overrides the default security manager before starting execution of test cases. */
    @BeforeAll
    static void beforeAll() {
        System.setSecurityManager(new DoNotExitJVM());
    }

    /** Restores the default security manager after executing all test cases. */
    @AfterAll
    static void afterAll() {
        System.setSecurityManager(null);
    }

    /** Custom exception to be thrown whe never {@link System#exit(int)} is invoked. */
    private static class ExitException extends SecurityException {
        public final int status;

        public ExitException(int status) {
            this.status = status;
        }
    }

    /** Override {@link SecurityManager} to prevent exiting JVM */
    private static class DoNotExitJVM extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {}

        @Override
        public void checkPermission(Permission perm, Object context) {}

        /**
         * Throws an {@link ExitException} instead of exiting the JVM.
         *
         * @param status Exit code of the invocation
         */
        @Override
        public void checkExit(int status) {
            super.checkExit(status);
            throw new ExitException(status);
        }
    }

    @Test
    void should_exitWithZero_withNonEmptyOutput(@TempDir Path tempDir) throws IOException {
        // arrange
        Path outputJson = tempDir.resolve("output.json");
        String[] classpath =
                TestHelper.getMavenClasspathFromBuildDirectory(
                        TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
        String[] args = {
            "-i",
            TestHelper.PATH_TO_BREAKPOINT_INPUT.resolve("basic-math.txt").toString(),
            "-p",
            StringUtils.join(classpath, " "),
            "-t",
            "foo.BasicMathTest::test_add foo.BasicMathTest::test_subtract",
            "-o",
            outputJson.toString()
        };

        // assert
        ExitException exit = assertThrows(ExitException.class, () -> Collector.main(args));
        assertEquals(0, exit.status);
        assertNonEmptyFile(outputJson);
    }

    private static void assertNonEmptyFile(Path pathToFile) throws IOException {
        assertThat(pathToFile.toFile(), anExistingFile());
        Reader reader = new FileReader(pathToFile.toFile());
        int fileSize = reader.read();
        assertNotEquals(-1, fileSize);
    }

    @Test
    void should_exitWithNonZeroCode(@TempDir Path tempDir) {
        // arrange
        Path outputJson = tempDir.resolve("output.json");
        String[] classpath =
                TestHelper.getMavenClasspathFromBuildDirectory(
                        TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("without-debug"));
        String[] args = {
            "-i",
            TestHelper.PATH_TO_BREAKPOINT_INPUT.resolve("basic-math.txt").toString(),
            "-p",
            StringUtils.join(classpath, " "),
            "-t",
            "foo.BasicMathTest::test_add foo.BasicMathTest::test_subtract",
            "-o",
            outputJson.toString(),
        };

        // assert
        ExitException exit = assertThrows(ExitException.class, () -> Collector.main(args));
        assertNotEquals(0, exit.status);
        assertThat(outputJson.toFile(), not(anExistingFile()));
    }

    @Nested
    class RecordOnlyWhatIsAskedFor {
        @Test
        void onlyBreakpointsShouldBeRecorded(@TempDir Path tempDir) throws IOException {
            // arrange
            Path outputJson = tempDir.resolve("output.json");
            String[] classpath =
                    TestHelper.getMavenClasspathFromBuildDirectory(
                            TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
            String[] args = {
                "-i",
                TestHelper.PATH_TO_BREAKPOINT_INPUT.resolve("basic-math.txt").toString(),
                "-p",
                StringUtils.join(classpath, " "),
                "-t",
                "foo.BasicMathTest::test_add foo.BasicMathTest::test_subtract",
                "-o",
                outputJson.toString()
            };

            // act
            assertThrows(ExitException.class, () -> Collector.main(args));

            // assert
            assertNonEmptyFile(outputJson);
            try (JsonReader jsonReader = new JsonReader(new FileReader(outputJson.toFile()))) {
                final Gson gson = new Gson();
                Object json = gson.fromJson(jsonReader, Object.class);

                assertThat(((LinkedTreeMap) json).size(), equalTo(1));
                assertTrue(((LinkedTreeMap) json).containsKey("breakpoint"));
            }
        }

        @Test
        void onlyReturnDataShouldBeRecorded(@TempDir Path tempDir) throws IOException {
            // arrange
            Path outputJson = tempDir.resolve("output.json");
            String[] classpath =
                    TestHelper.getMavenClasspathFromBuildDirectory(
                            TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
            String[] args = {
                "-m",
                TestHelper.PATH_TO_RETURN_INPUT.resolve("basic-math.json").toString(),
                "-p",
                StringUtils.join(classpath, " "),
                "-t",
                "foo.BasicMathTest::test_add",
                "-o",
                outputJson.toString()
            };

            // act
            assertThrows(ExitException.class, () -> Collector.main(args));

            // assert
            assertNonEmptyFile(outputJson);
            try (JsonReader jsonReader = new JsonReader(new FileReader(outputJson.toFile()))) {
                final Gson gson = new Gson();
                Object json = gson.fromJson(jsonReader, Object.class);

                assertThat(((LinkedTreeMap<?, ?>) json).size(), equalTo(1));
                assertTrue(((LinkedTreeMap<?, ?>) json).containsKey("return"));
            }
        }

        @Test
        void breakpointAndReturnDataBothShouldBeRecorded(@TempDir Path tempDir) throws IOException {
            // arrange
            Path outputJson = tempDir.resolve("output.json");
            String[] classpath =
                    TestHelper.getMavenClasspathFromBuildDirectory(
                            TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
            String[] args = {
                "-i",
                TestHelper.PATH_TO_BREAKPOINT_INPUT.resolve("basic-math.txt").toString(),
                "-m",
                TestHelper.PATH_TO_RETURN_INPUT.resolve("basic-math.json").toString(),
                "-p",
                StringUtils.join(classpath, " "),
                "-t",
                "foo.BasicMathTest::test_add",
                "-o",
                outputJson.toString()
            };

            // act
            assertThrows(ExitException.class, () -> Collector.main(args));

            // assert
            assertNonEmptyFile(outputJson);
            try (JsonReader jsonReader = new JsonReader(new FileReader(outputJson.toFile()))) {
                final Gson gson = new Gson();
                Object json = gson.fromJson(jsonReader, Object.class);

                assertThat(((LinkedTreeMap<?, ?>) json).size(), equalTo(2));
                assertTrue(((LinkedTreeMap<?, ?>) json).containsKey("return"));
                assertTrue(((LinkedTreeMap<?, ?>) json).containsKey("breakpoint"));
            }
        }
    }

    @Test
    void shouldNotFailEvenIfZeroBreakpointsAreProvided(@TempDir Path tempDir) throws IOException {
        // arrange
        Path outputJson = tempDir.resolve("output.json");
        String[] classpath =
                TestHelper.getMavenClasspathFromBuildDirectory(
                        TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
        String[] args = {
            "-i",
            TestHelper.PATH_TO_BREAKPOINT_INPUT
                    .resolve("zero-breakpoints-basic-math.txt")
                    .toString(),
            "-p",
            StringUtils.join(classpath, " "),
            "-t",
            "foo.BasicMathTest::test_add",
            "-o",
            outputJson.toString()
        };

        // act
        assertThrows(ExitException.class, () -> Collector.main(args));

        // assert
        try (JsonReader jsonReader = new JsonReader(new FileReader(outputJson.toFile()))) {
            final Gson gson = new Gson();
            Object json = gson.fromJson(jsonReader, Object.class);
            assertThat(((LinkedTreeMap<?, ?>) json).size(), equalTo(0));
        }
    }
}
