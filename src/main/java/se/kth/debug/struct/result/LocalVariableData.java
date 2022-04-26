package se.kth.debug.struct.result;

import java.util.List;

public class LocalVariableData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.LOCAL_VARIABLE;
    private final String name;
    private List<FieldData> nestedObjects = null;
    private ValueWrapper value;

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

    @Override
    public ValueWrapper getValueWrapper() {
        return value;
    }

    @Override
    public void setValue(ValueWrapper newValue) {
        value = newValue;
    }
}
