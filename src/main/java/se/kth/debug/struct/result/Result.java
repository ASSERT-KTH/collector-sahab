package se.kth.debug.struct.result;

import java.util.List;

public class Result {
    private final String file;
    private final int lineNumber;
    private final List<StackFrameContext> stackFrameContexts;

    public Result(String file, int lineNumber, List<StackFrameContext> stackFrameContexts) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.stackFrameContexts = stackFrameContexts;
    }
}
