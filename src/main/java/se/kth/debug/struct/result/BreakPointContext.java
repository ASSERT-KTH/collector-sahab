package se.kth.debug.struct.result;

import java.util.List;

public class BreakPointContext {
    private final String file;
    private final int lineNumber;
    private final List<StackFrameContext> stackFrameContexts;

    public BreakPointContext(String file, int lineNumber, List<StackFrameContext> stackFrameContexts) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.stackFrameContexts = stackFrameContexts;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public List<StackFrameContext> getStackFrameContexts() {
        return stackFrameContexts;
    }
}
