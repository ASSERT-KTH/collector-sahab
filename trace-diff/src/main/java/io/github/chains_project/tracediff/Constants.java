package io.github.chains_project.tracediff;

import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final String EXEC_DIFF_COMMAND_NAME = "EXEC-DIFF";

    public static final String EXEC_FREQ_DIFF_COMMAND_NAME = "freq";
    public static final String ARG_SLUG = "--slug";
    public static final String ARG_COMMIT = "--commit";
    public static final String ARG_ORIGINAL_PATH = "--original-path";
    public static final String ARG_PATCHED_PATH = "--patched-path";
    public static final String ARG_OUTPUT_PATH = "--output-path";
    public static final String ARG_FULL_REPORT_LINK = "--full-report-link";
    public static final String ARG_CHANGED_FILE_PATH = "--changed-file-path";
    public static final String ARG_SELECTED_TESTS = "--selected-tests";
    public static final String ARG_EXCLUDE_RANDOM_VALUES = "--exclude-random-values";

    public static final String STATE_DIFF_COMMAND_NAME = "sdiff";
    public static final String ARG_LEFT_REPORT_PATH = "--left-report-path";
    public static final String ARG_RIGHT_REPORT_PATH = "--right-report-path";
    public static final String ARG_LEFT_SRC_PATH = "--left-src-path";
    public static final String ARG_RIGHT_SRC_PATH = "--right-src-path";
    public static final String ARG_TRACE_DIFF_FULL_REPORT_PATH = "--trace-diff-report-path";
    public static final String ARG_TEST_LINK = "--test-link";

    public static final String TEST_SEPARATOR = ";";
    public static final String TEST_METHOD_NAME_SEPARATOR = "::";
    public static final String UNKNOWN_TEST = "unknown::unknown";
    public static final int REPEATS_FOR_RANDOM_EXCLUSION = 3;
<<<<<<< HEAD:trace-diff/src/main/java/io/github/chains_project/tracediff/Constants.java
    public static final List<String> FILE_RELATED_CLASSES = Arrays.asList("java.io.File", "java.nio.file.Path", "sun.nio.fs.UnixPath",
            "sun.nio.fs.WindowsPath");
=======
>>>>>>> chore: updated integration between main and diff-computer:trace-diff/src/main/java/se/assertkth/tracediff/Constants.java
}
