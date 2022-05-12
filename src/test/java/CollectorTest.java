import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.kth.debug.Collector;

public class CollectorTest {
    @Test
    void should_runWithoutErrors_withNonEmptyOutput(@TempDir Path tempDir) throws IOException {
        // arrange
        Path outputJson = tempDir.resolve("output.json");
        String[] classpath =
                TestHelper.getMavenClasspathFromBuildDirectory(
                        TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
        String[] args = {
            "-i",
            TestHelper.PATH_TO_INPUT.resolve("basic-math.txt").toString(),
            "-p",
            StringUtils.join(classpath, " "),
            "-t",
            "foo.BasicMathTest::test_add foo.BasicMathTest::test_subtract",
            "-o",
            outputJson.toString()
        };

        // act
        Collector.main(args);

        // assert
        assertNonEmptyFile(outputJson);
    }

    private static void assertNonEmptyFile(Path pathToFile) throws IOException {
        assertThat(pathToFile.toFile(), anExistingFile());
        Reader reader = new FileReader(pathToFile.toFile());
        int fileSize = reader.read();
        assertNotEquals(-1, fileSize);
    }

    @Test
    void should_throwAbsentInformationException(@TempDir Path tempDir)
            throws FileNotFoundException {
        // arrange
        Path outputJson = tempDir.resolve("output.json");
        String[] classpath =
                TestHelper.getMavenClasspathFromBuildDirectory(
                        TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT_WITHOUT_DEBUG_INFO.resolve(
                                "without-debug"));
        String[] args = {
            "-i",
            TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT_WITHOUT_DEBUG_INFO
                    .resolve("basic-math.txt")
                    .toString(),
            "-p",
            StringUtils.join(classpath, " "),
            "-t",
            "foo.BasicMathTest::test_add foo.BasicMathTest::test_subtract",
            "-o",
            outputJson.toString(),
        };

        // assert
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errorStream));
        Collector.main(args);

        assertThat(
                errorStream.toString(), containsString("com.sun.jdi.AbsentInformationException"));
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
                TestHelper.PATH_TO_INPUT.resolve("basic-math.txt").toString(),
                "-p",
                StringUtils.join(classpath, " "),
                "-t",
                "foo.BasicMathTest::test_add foo.BasicMathTest::test_subtract",
                "-o",
                outputJson.toString(),
                "--skip-return-values"
            };

            // act
            Collector.main(args);

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
                "-i",
                TestHelper.PATH_TO_INPUT.resolve("basic-math.txt").toString(),
                "-p",
                StringUtils.join(classpath, " "),
                "-t",
                "foo.BasicMathTest::test_add",
                "-o",
                outputJson.toString(),
                "--skip-breakpoint-values"
            };

            // act
            Collector.main(args);

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
                TestHelper.PATH_TO_INPUT.resolve("basic-math.txt").toString(),
                "-p",
                StringUtils.join(classpath, " "),
                "-t",
                "foo.BasicMathTest::test_add",
                "-o",
                outputJson.toString()
            };

            // act
            Collector.main(args);

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
            TestHelper.PATH_TO_INPUT.resolve("zero-breakpoints-basic-math.txt").toString(),
            "-p",
            StringUtils.join(classpath, " "),
            "-t",
            "foo.BasicMathTest::test_add",
            "-o",
            outputJson.toString()
        };

        // act
        Collector.main(args);

        // assert
        assertNonEmptyFile(outputJson);
    }

    @Nested
    @Disabled
    class BothAttributeShouldBePresent {
        @Test
        void breakpointShouldBePresentEvenIfItsDataIsEmpty(@TempDir Path tempDir)
                throws IOException {
            // arrange
            Path outputJson = tempDir.resolve("output.json");
            String[] classpath =
                    TestHelper.getMavenClasspathFromBuildDirectory(
                            TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
            String[] args = {
                "-i",
                TestHelper.PATH_TO_INPUT.resolve("zero-breakpoints-basic-math.txt").toString(),
                "-p",
                StringUtils.join(classpath, " "),
                "-t",
                "foo.BasicMathTest::test_add",
                "-o",
                outputJson.toString()
            };

            // act
            Collector.main(args);

            // assert
            try (JsonReader jsonReader = new JsonReader(new FileReader(outputJson.toFile()))) {
                final Gson gson = new Gson();
                Object json = gson.fromJson(jsonReader, Object.class);
                assertThat(((LinkedTreeMap<?, ?>) json).size(), equalTo(2));
                assertThat((List<?>) (((LinkedTreeMap<?, ?>) json).get("breakpoint")), is(empty()));
                assertThat(
                        (List<?>) (((LinkedTreeMap<?, ?>) json).get("return")), is(not(empty())));
            }
        }

        @Test
        void returnShouldBePresentEvenIfItsDataIsEmpty(@TempDir Path tempDir) throws IOException {
            // arrange
            Path outputJson = tempDir.resolve("output.json");
            String[] classpath =
                    TestHelper.getMavenClasspathFromBuildDirectory(
                            TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
            String[] args = {
                "-i",
                TestHelper.PATH_TO_INPUT.resolve("void-method.txt").toString(),
                "-p",
                StringUtils.join(classpath, " "),
                "-t",
                "foo.VoidMethodTest::test_doNothing",
                "-o",
                outputJson.toString()
            };

            // act
            Collector.main(args);

            // assert
            try (JsonReader jsonReader = new JsonReader(new FileReader(outputJson.toFile()))) {
                final Gson gson = new Gson();
                Object json = gson.fromJson(jsonReader, Object.class);
                assertThat(((LinkedTreeMap<?, ?>) json).size(), equalTo(2));
                assertThat(
                        (List<?>) (((LinkedTreeMap<?, ?>) json).get("breakpoint")),
                        is(not(empty())));
                assertThat(
                        (List<?>) (((LinkedTreeMap<?, ?>) json).get("return")), is(not(empty())));
            }
        }
    }

    @Test
    void gson_shouldBeAbleToSerialise_specialFloatingPointValue(@TempDir Path tempDir)
            throws IOException {
        // arrange
        Path outputJson = tempDir.resolve("output.json");
        String[] classpath =
                TestHelper.getMavenClasspathFromBuildDirectory(
                        TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
        String[] args = {
            "-i",
            TestHelper.PATH_TO_INPUT.resolve("special-floating-point-value.txt").toString(),
            "-p",
            StringUtils.join(classpath, " "),
            "-t",
            "foo.SpecialFloatingPointValueTest::test_generateNaN",
            "-o",
            outputJson.toString()
        };

        // act
        Collector.main(args);

        // assert
        String expectedOutput =
                Files.readString(
                        TestHelper.PATH_TO_EXPECTED_OUTPUT.resolve(
                                "special-floating-point-value.json"));
        String actualOutput = Files.readString(outputJson);
        assertThat(actualOutput, equalTo(expectedOutput));
    }
}
