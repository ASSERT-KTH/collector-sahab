package se.kth.debug;

public class CollectorOptions {
    private int stackTraceDepth;
    private int numberOfArrayElements;
    private int executionDepth;
    private boolean skipPrintingField;
    private boolean skipReturnValues;
    private boolean skipBreakpointValues;

    public int getStackTraceDepth() {
        return stackTraceDepth;
    }

    public int getNumberOfArrayElements() {
        return numberOfArrayElements;
    }

    public int getExecutionDepth() {
        return executionDepth;
    }

    public boolean shouldSkipPrintingField() {
        return skipPrintingField;
    }

    public boolean shouldSkipBreakpointValues() {
        return skipBreakpointValues;
    }

    public boolean shouldSkipReturnValues() {
        return skipReturnValues;
    }

    public void setStackTraceDepth(int stackTraceDepth) {
        this.stackTraceDepth = stackTraceDepth;
    }

    public void setNumberOfArrayElements(int numberOfArrayElements) {
        this.numberOfArrayElements = numberOfArrayElements;
    }

    public void setExecutionDepth(int executionDepth) {
        this.executionDepth = executionDepth;
    }

    public void setSkipPrintingField(boolean skipPrintingField) {
        this.skipPrintingField = skipPrintingField;
    }

    public CollectorOptions setSkipReturnValues(boolean skipReturnValues) {
        this.skipReturnValues = skipReturnValues;
        return this;
    }

    public CollectorOptions setSkipBreakpointValues(boolean skipBreakpointValues) {
        this.skipBreakpointValues = skipBreakpointValues;
        return this;
    }
}
