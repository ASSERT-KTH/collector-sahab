package se.kth.debug.struct.result;

import java.util.List;

public class LocalVariableData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.LOCAL_VARIABLE;
    private Long id;
    private final String name;
    private final String type;
    private List<FieldData> nestedTypes = null;
    private String value;

    public LocalVariableData(Long id, String name, String type, String value) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public void setNestedTypes(List<FieldData> nestedTypes) {
        this.nestedTypes = nestedTypes;
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
