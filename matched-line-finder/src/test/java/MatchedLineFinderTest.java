import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chains_project.cs.commons.FileAndBreakpoint;
import io.github.chains_project.cs.commons.MethodForExitEvent;
import io.github.chains_project.mlf.MatchedLineFinder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class MatchedLineFinderTest {
    static final Path BASE_DIR = Paths.get("src/test/resources/matched-line-finder");

    private static final List<String> IGNORE_TESTS = List.of(
            // ToDo: Can be fixed after
            // https://github.com/SpoonLabs/gumtree-spoon-ast-diff/issues/245
            "nested-lambda", "anonymous-class", "nested-anonymous-class", "multiple-nested-types");

    @ParameterizedTest
    @ArgumentsSource(ResourceProvider.Patch.class)
    void should_correctlyGenerateAllInputFilesForCollectorSahab(ResourceProvider.TestResource sources)
            throws Exception {
        if (IGNORE_TESTS.contains(sources.dir)) {
            assumeTrue(false);
        }
        Triple<String, String, String> inputsForCollectorSahab = MatchedLineFinder.invoke(sources.left, sources.right);
        assertInputsAreAsExpected(inputsForCollectorSahab, sources.expected);
    }

    private void assertInputsAreAsExpected(Triple<String, String, String> input, Path dirContainingExpectedFiles)
            throws IOException {
        List<FileAndBreakpoint> actualBreakpointLeft = deserialiseFileAndBreakpoint(input.getLeft());
        List<FileAndBreakpoint> actualBreakpointRight = deserialiseFileAndBreakpoint(input.getRight());

        List<FileAndBreakpoint> expectedBreakpointLeft =
                deserialiseFileAndBreakpoint(Files.readString(dirContainingExpectedFiles.resolve("input-left.txt")));
        List<FileAndBreakpoint> expectedBreakpointRight =
                deserialiseFileAndBreakpoint(Files.readString(dirContainingExpectedFiles.resolve("input-right.txt")));

        if (dirContainingExpectedFiles.resolve("methods.json").toFile().exists()) {
            List<MethodForExitEvent> actualMethods = deserialiseMethodForExitEvent(input.getMiddle());
            List<MethodForExitEvent> expectedMethods =
                    deserialiseMethodForExitEvent(Files.readString(dirContainingExpectedFiles.resolve("methods.json")));

            assertThat(actualMethods, containsInAnyOrder(expectedMethods.toArray(new MethodForExitEvent[0])));
        }

        assertThat(actualBreakpointLeft, containsInAnyOrder(expectedBreakpointLeft.toArray(new FileAndBreakpoint[0])));
        assertThat(
                actualBreakpointRight, containsInAnyOrder(expectedBreakpointRight.toArray(new FileAndBreakpoint[0])));
    }

    private List<FileAndBreakpoint> deserialiseFileAndBreakpoint(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(
                json, mapper.getTypeFactory().constructCollectionType(List.class, FileAndBreakpoint.class));
    }

    private List<MethodForExitEvent> deserialiseMethodForExitEvent(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(
                json, mapper.getTypeFactory().constructCollectionType(List.class, MethodForExitEvent.class));
    }

    @Test
    void throwsNoDiffException_whenThereIsNoDiffLinePresent() {
        // arrange
        File left = BASE_DIR.resolve("EXCLUDE_no-diff").resolve("left.java").toFile();
        File right = BASE_DIR.resolve("EXCLUDE_no-diff").resolve("right.java").toFile();

        // assert
        assertThrowsExactly(MatchedLineFinder.NoDiffException.class, () -> MatchedLineFinder.invoke(left, right));
    }
}

class ResourceProvider {
    static class TestResource {
        String dir;
        File left;
        File right;
        Path expected;

        private TestResource(String dir, File left, File right, Path expected) {
            this.dir = dir;
            this.left = left;
            this.right = right;
            this.expected = expected;
        }

        private static TestResource fromTestDirectory(File testDir) {
            String dir = testDir.getName();
            File left = testDir.toPath().resolve("left.java").toFile();
            File right = testDir.toPath().resolve("right.java").toFile();
            Path expected = testDir.toPath().resolve("expected");
            return new TestResource(dir, left, right, expected);
        }

        @Override
        public String toString() {
            return dir;
        }
    }

    static class Patch implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Arrays.stream(Objects.requireNonNull(
                            MatchedLineFinderTest.BASE_DIR.toFile().listFiles()))
                    .filter(File::isDirectory)
                    .filter(dir -> !dir.getName().startsWith("EXCLUDE_"))
                    .map(TestResource::fromTestDirectory)
                    .map(Arguments::of);
        }
    }
}
