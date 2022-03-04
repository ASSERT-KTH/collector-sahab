package se.kth.debug.struct.result;

import java.util.List;

public interface RuntimeValueCollection extends RuntimeValue {
    String getIdentifier();

    void addData(RuntimeValue runtimeValue);

    List<RuntimeValue> getCollection();
}
