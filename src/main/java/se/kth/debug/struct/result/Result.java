package se.kth.debug.struct.result;

import java.util.List;

public class Result {
    private final String file;
    private final int lineNumber;
    private final List<Statistics> runtimeValues;

    public Result(String file, int lineNumber, List<Statistics> runtimeValues) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.runtimeValues = runtimeValues;
    }
}
