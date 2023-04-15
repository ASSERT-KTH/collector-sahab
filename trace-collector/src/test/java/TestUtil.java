import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import se.assertkth.collector.CollectorAgent;

public class TestUtil {
    private TestUtil() {}

    /**
     * Returns the path of the trace-collector.jar file.
     *
     * Fetches from target/classes in test environment.
     */
    static String getAgentPath() throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path traceCollector = Path.of(tempDir, "trace-collector.jar");

        try (InputStream traceCollectorStream = CollectorAgent.class.getResourceAsStream("/trace-collector.jar")) {
            Files.copy(traceCollectorStream, traceCollector, StandardCopyOption.REPLACE_EXISTING);
        }

        return traceCollector.toAbsolutePath().toString();
    }
}
