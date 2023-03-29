package se.kth.debug.struct.result;

import java.util.ArrayList;
import java.util.List;

public class StackFrameContext {
    private final int positionFromTopInStackTrace;
    private final String location;
    private final List<String> stackTrace;

    private List<RuntimeValue> runtimeValueCollection = new ArrayList<>();

    public StackFrameContext(int positionFromTopInStackTrace, String location, List<String> stackTrace) {
        this.positionFromTopInStackTrace = positionFromTopInStackTrace;
        this.location = location;
        this.stackTrace = stackTrace;
    }

    public void addRuntimeValueCollection(List<? extends RuntimeValue> runtimeValues) {
        this.runtimeValueCollection.addAll(runtimeValues);
    }

    public List<RuntimeValue> getRuntimeValueCollection() {
        return runtimeValueCollection;
    }
}
