package se.kth.debug.struct.result;

import java.util.List;

public class FieldData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.FIELD;
    private final String name;
    private final String type;
    private final Object value;
    private List<FieldData> nestedObjects = null;

    public FieldData(String name, String type, Object value) {
        this.name = name;
        this.type = type;
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
    public Object getValue() {
        return value;
    }
}
