package se.assertteam;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LineSnapshot {

    private final String file;
    private final int lineNumber;
    private final List<StackFrameContext> stackFrameContext;

    public LineSnapshot(@JsonProperty("file") String file, @JsonProperty("lineNumber") int lineNumber, @JsonProperty("stackFrameContext") List<StackFrameContext> stackFrameContext) {
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
