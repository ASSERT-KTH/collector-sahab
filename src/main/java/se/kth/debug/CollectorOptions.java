package se.kth.debug;

public class CollectorOptions {
    private int stackTraceDepth;
    private int numberOfArrayElements;
    private int executionDepth;
    private boolean skipPrintingField;

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
}
