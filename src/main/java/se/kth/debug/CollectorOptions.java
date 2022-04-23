package se.kth.debug;

public class CollectorOptions {
    private int objectDepth;
    private int stackTraceDepth;
    private int numberOfArrayElements;
    private int arrayDepth;
    private boolean skipPrintingField;

    public int getObjectDepth() {
        return objectDepth;
    }

    public int getStackTraceDepth() {
        return stackTraceDepth;
    }

    public int getNumberOfArrayElements() {
        return numberOfArrayElements;
    }

    public int getArrayDepth() {
        return arrayDepth;
    }

    public boolean shouldSkipPrintingField() {
        return skipPrintingField;
    }

    public void setObjectDepth(int objectDepth) {
        this.objectDepth = objectDepth;
    }

    public void setStackTraceDepth(int stackTraceDepth) {
        this.stackTraceDepth = stackTraceDepth;
    }

    public void setNumberOfArrayElements(int numberOfArrayElements) {
        this.numberOfArrayElements = numberOfArrayElements;
    }

    public void setArrayDepth(int arrayDepth) {
        this.arrayDepth = arrayDepth;
    }

    public void setSkipPrintingField(boolean skipPrintingField) {
        this.skipPrintingField = skipPrintingField;
    }
}
