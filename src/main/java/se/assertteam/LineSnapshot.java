package se.assertteam;

import java.util.List;

public class LineSnapshot {

    private final String file;
    private final int lineNumber;
    private final List<StackFrameContext> stackFrameContext;

    public LineSnapshot(String file, int lineNumber, List<StackFrameContext> stackFrameContext) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.stackFrameContext = stackFrameContext;
    }

    public String getFile() {
        return file;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public List<StackFrameContext> getStackFrameContext() {
        return stackFrameContext;
    }
}
