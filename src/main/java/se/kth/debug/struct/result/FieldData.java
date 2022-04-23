package se.kth.debug.struct.result;

import java.util.List;

public class FieldData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.FIELD;
    private final String name;
    private transient Long id = null;
    private List<FieldData> nestedObjects = null;
    private ValueWrapper value;

    public FieldData(Long id, String name, ValueWrapper value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public FieldData(String name, ValueWrapper value) {
        this.name = name;
        this.value = value;
    }

    public void setNestedObjects(List<FieldData> nestedObjects) {
        this.nestedObjects = nestedObjects;
    }

    @Override
    public RuntimeValueKind getKind() {
        return kind;
    }

    public Long getID() {
        return id;
    }

    @Override
    public ValueWrapper getValueWrapper() {
        return value;
    }

    @Override
    public void setValue(ValueWrapper newValue) {
        value = newValue;
    }
}
