package io.github.chains_project.collector.util;

import io.github.chains_project.collector.CollectorAgent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class JavaAgentPath {
    private JavaAgentPath() {}

    /**
     * Returns the path of the trace-collector.jar file in target/classes directory of `trace-collector` module.
     */
    public static String getAgentPath() throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path traceCollector = Path.of(tempDir, "trace-collector.jar");

        try (InputStream traceCollectorStream = CollectorAgent.class.getResourceAsStream("/trace-collector.jar")) {
            Files.copy(traceCollectorStream, traceCollector, StandardCopyOption.REPLACE_EXISTING);
        }

        return traceCollector.toAbsolutePath().toString();
    }
}
