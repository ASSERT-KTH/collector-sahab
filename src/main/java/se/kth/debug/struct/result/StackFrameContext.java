package se.kth.debug.struct.result;

import java.util.ArrayList;
import java.util.List;

public class StackFrameContext {
    private int positionFromTopInStackTrace;
    private String location;
    private List<RuntimeValueCollection> runtimeValueCollection = new ArrayList<>();

    public StackFrameContext(int positionFromTopInStackTrace, String location) {
        this.positionFromTopInStackTrace = positionFromTopInStackTrace;
        this.location = location;
    }

    public void addRuntimeValue(RuntimeValueCollection runtimeValueCollection) {
        this.runtimeValueCollection.add(runtimeValueCollection);
    }
}
