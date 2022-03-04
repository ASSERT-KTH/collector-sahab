package se.kth.debug.struct.result;

import java.util.ArrayList;
import java.util.List;

public class LocalVariableList implements RuntimeValueTypeChunk<LocalVariable> {
    private static final String IDENTIFIER = "Local variables";
    private final List<LocalVariable> localVariables = new ArrayList<>();

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public void addData(String name, String value) {
        localVariables.add(new LocalVariable(name, value));
    }

    @Override
    public List<LocalVariable> getCollection() {
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
