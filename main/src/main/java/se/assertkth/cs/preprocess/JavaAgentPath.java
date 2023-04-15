package se.assertkth.cs.preprocess;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import se.assertkth.cs.Main;

public class JavaAgentPath {
    private JavaAgentPath() {}

    /**
     * Returns the path of the trace-collector.jar file.
     *
     * Fetches it from within the collector-sahab-{verison}-jar-with-dependencies.jar
     * in production environment.
     */
    public static String getAgentPath() throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path traceCollector = Path.of(tempDir, "trace-collector.jar");

        try (InputStream traceCollectorStream = Main.class.getResourceAsStream("/trace-collector.jar")) {
            Files.copy(traceCollectorStream, traceCollector, StandardCopyOption.REPLACE_EXISTING);
        }

        return traceCollector.toAbsolutePath().toString();
    }
}
