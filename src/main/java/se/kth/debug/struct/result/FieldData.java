package se.kth.debug.struct.result;

import java.util.List;

public class FieldData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.FIELD;
    private final String name;
    private final String type;
    private transient Long id = null;
    private List<FieldData> nestedObjects = null;
    private Object value;

    public FieldData(Long id, String name, String type, Object value) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.value = value;
    }

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

    public Long getID() {
        return id;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object newValue) {
        value = newValue;
    }
}
