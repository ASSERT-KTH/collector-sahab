package se.assertkth.collector.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import se.assertkth.collector.CollectorAgent;

public class JavaAgentPath {
    private JavaAgentPath() {}

    /**
     * Returns the path of the trace-collector.jar file in target/classes directory of `trace-collector` module.
     */
    public static String getAgentPath() throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path traceCollector = Path.of(tempDir, "trace-collector.jar");

        if (Files.exists(traceCollector)) {
            return traceCollector.toAbsolutePath().toString();
        }

        try (InputStream traceCollectorStream = CollectorAgent.class.getResourceAsStream("/trace-collector.jar")) {
            Files.copy(traceCollectorStream, traceCollector, StandardCopyOption.REPLACE_EXISTING);
        }

        return traceCollector.toAbsolutePath().toString();
    }
}