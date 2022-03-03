package se.kth.debug;

import com.google.gson.annotations.Expose;

import java.util.List;

public class Result {
    @Expose private final String file;
    @Expose private final int lineNumber;
    @Expose private final List<Object> values;

    public Result(String file, int lineNumber, List<Object> values) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.values = values;
    }
}
