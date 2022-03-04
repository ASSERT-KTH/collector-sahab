package se.kth.debug.struct.result.localVariable;

import se.kth.debug.struct.result.RuntimeValue;

import java.util.ArrayList;
import java.util.List;

public class LocalVariableListImpl implements LocalVariableList {
    private static final String IDENTIFIER = "Local variables";
    private final List<RuntimeValue> localVariables = new ArrayList<>();

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public void addData(String name, String value) {
        localVariables.add(new LocalVariable(name, value));
    }

    public void addData(RuntimeValue runtimeValue) {
        localVariables.add(runtimeValue);
    }

    @Override
    public List<RuntimeValue> getCollection() {
        return localVariables;
    }

}

class LocalVariable implements RuntimeValue {
    private final String name;
    private final String value;

    LocalVariable(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
