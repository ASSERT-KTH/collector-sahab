package io.github.chains_project.tracediff;

public class Constants {
    public static final String EXEC_DIFF_COMMAND_NAME = "EXEC-DIFF";

    public static final String EXEC_FREQ_DIFF_COMMAND_NAME = "freq";
    public static final String ARG_SLUG = "--slug";
    public static final String ARG_COMMIT = "--commit";
    public static final String ARG_ORIGINAL_PATH = "--original-path";
    public static final String ARG_PATCHED_PATH = "--patched-path";
    public static final String ARG_OUTPUT_PATH = "--output-path";
    public static final String ARG_ALL_DIFFS_REPORT_PATH = "--all-diffs-report-path";
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
}
