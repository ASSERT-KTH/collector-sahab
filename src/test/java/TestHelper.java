import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import se.kth.debug.CollectorOptions;

public class TestHelper {
    public static final Path PATH_TO_SAMPLE_MAVEN_PROJECT =
            Paths.get("src/test/resources/sample-maven-project");
    private static final Path PATH_TO_SAMPLE_MAVEN_PROJECT_RESOURCES =
            PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("src").resolve("test").resolve("resources");

    public static final Path PATH_TO_INPUT =
            PATH_TO_SAMPLE_MAVEN_PROJECT_RESOURCES.resolve("inputs");

    public static final Path PATH_TO_EXPECTED_OUTPUT =
            PATH_TO_SAMPLE_MAVEN_PROJECT_RESOURCES.resolve("expected-outputs");

    public static final Path PATH_TO_SAMPLE_MAVEN_PROJECT_WITHOUT_DEBUG_INFO =
            Paths.get("src/test/resources/sample-maven-project-cannot-be-debugged");

    public static String[] getMavenClasspathFromBuildDirectory(Path buildDirectory)
            throws FileNotFoundException {
        if (!Files.exists(buildDirectory)) {
            throw new FileNotFoundException(buildDirectory + " does not exist");
        }
        List<String> classpath =
                new ArrayList<>(
                        List.of(
                                buildDirectory.resolve("classes").toString(),
                                buildDirectory.resolve("test-classes").toString()));
        try {
            String additionalClasspath = Files.readString(buildDirectory.resolve("cp.txt"));
            classpath.addAll(
                    Arrays.stream(additionalClasspath.split(":")).collect(Collectors.toList()));
            return classpath.toArray(new String[] {});
        } catch (IOException e) {
            throw new RuntimeException("Classpath of " + buildDirectory + " could not be read.");
        }
    }

    public static CollectorOptions getDefaultOptions() {
        CollectorOptions context = new CollectorOptions();
        context.setStackTraceDepth(1);
        context.setNumberOfArrayElements(10);
        context.setExecutionDepth(0);
        context.setSkipPrintingField(false);
        return context;
    }
}
