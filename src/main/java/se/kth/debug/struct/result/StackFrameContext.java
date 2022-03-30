package se.kth.debug.struct.result;

import com.google.gson.annotations.JsonAdapter;
import java.util.ArrayList;
import java.util.List;
import se.kth.debug.RuntimeValueAdapter;

public class StackFrameContext {
    private final int positionFromTopInStackTrace;
    private final String location;
    private final List<String> stackTrace;

    @JsonAdapter(RuntimeValueAdapter.class)
    private List<RuntimeValue> runtimeValueCollection = new ArrayList<>();

    public StackFrameContext(
            int positionFromTopInStackTrace, String location, List<String> stackTrace) {
        this.positionFromTopInStackTrace = positionFromTopInStackTrace;
        this.location = location;
        this.stackTrace = stackTrace;
    }

    public void addRuntimeValueCollection(List<? extends RuntimeValue> runtimeValues) {
        this.runtimeValueCollection.addAll(runtimeValues);
    }
}
