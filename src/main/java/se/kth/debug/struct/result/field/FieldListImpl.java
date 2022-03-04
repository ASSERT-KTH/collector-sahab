package se.kth.debug.struct.result.field;

import se.kth.debug.struct.result.RuntimeValue;

import java.util.ArrayList;
import java.util.List;

public class FieldListImpl implements FieldList {
    private static final String IDENTIFIER = "Fields";
    private final List<RuntimeValue> fields = new ArrayList<>();

    @Override
    public void addData(RuntimeValue runtimeValue) {
        fields.add(runtimeValue);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public List<RuntimeValue> getCollection() {
        return fields;
    }

    @Override
    public void addData(String name, String value) {
        fields.add(new Field(name, value));
    }
}

class Field implements RuntimeValue {
    private final String name;
    private final String value;

    Field(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
