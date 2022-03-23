import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class TestHelper {
    public static final Path PATH_TO_SAMPLE_MAVEN_PROJECT =
            Paths.get("src/test/resources/sample-maven-project");

    public static String getMavenClasspathFromBuildDirectory(Path buildDirectory) {
        List<String> classpath =
                List.of(
                        buildDirectory.resolve("classes").toString(),
                        buildDirectory.resolve("test-classes").toString(),
                        buildDirectory.resolve("dependency").toString());
        return StringUtils.join(classpath, " ");
    }
}
