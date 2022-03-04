package se.kth.debug.struct.result;

import java.util.ArrayList;
import java.util.List;

public class StackFrameContext {
    private int positionFromTopInStackTrace;
    private String location;
    private List<RuntimeValueTypeChunk<? extends RuntimeValue>> runtimeValues = new ArrayList<>();

    public StackFrameContext(int positionFromTopInStackTrace, String location) {
        this.positionFromTopInStackTrace = positionFromTopInStackTrace;
        this.location = location;
    }

    public void addRuntimeValue(RuntimeValueTypeChunk<? extends RuntimeValue> runtimeValue) {
        this.runtimeValues.add(runtimeValue);
    }
}
