package se.kth.debug.struct.result;

import java.util.List;

public class LocalVariableData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.LOCAL_VARIABLE;
    private final String name;
    private transient Long id = null;
    private List<FieldData> nestedObjects = null;
    private ValueWrapper value;

    public LocalVariableData(Long id, String name, ValueWrapper value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public LocalVariableData(String name, ValueWrapper value) {
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
