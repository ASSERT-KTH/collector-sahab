package se.assertkth.tracediff.trace.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

// ProcessHelper
public class PH {
    private static final Logger logger = LoggerFactory.getLogger(PH.class);

    public static int run(File dir, String message, String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.inheritIO();
        pb.directory(dir);
        logger.info(message);
        Process p = pb.start();
        return p.waitFor();
    }
}
