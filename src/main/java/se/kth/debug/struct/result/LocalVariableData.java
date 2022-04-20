package se.kth.debug.struct.result;

import java.util.List;

public class LocalVariableData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.LOCAL_VARIABLE;
    private final String name;
    private final String type;
    private transient Long id = null;
    private List<FieldData> nestedObjects = null;
    private String value;

    public LocalVariableData(Long id, String name, String type, String value) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public LocalVariableData(String name, String type, String value) {
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
    public String getValue() {
        return value;
    }

    public void setValue(String newValue) {
        value = newValue;
    }
}
