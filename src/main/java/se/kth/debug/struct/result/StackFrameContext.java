package se.kth.debug.struct.result;

import com.google.gson.annotations.JsonAdapter;
import java.util.ArrayList;
import java.util.List;
import se.kth.debug.RuntimeValueAdapter;

public class StackFrameContext {
    private int positionFromTopInStackTrace;
    private String location;

    @JsonAdapter(RuntimeValueAdapter.class)
    private List<RuntimeValue> runtimeValueCollection = new ArrayList<>();

    public StackFrameContext(int positionFromTopInStackTrace, String location) {
        this.positionFromTopInStackTrace = positionFromTopInStackTrace;
        this.location = location;
    }

    public void addRuntimeValueCollection(List<? extends RuntimeValue> runtimeValues) {
        this.runtimeValueCollection.addAll(runtimeValues);
    }
}
