package se.kth.debug.struct.result;

import java.util.List;

public class FieldData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.FIELD;
    private final Long id;
    private final String name;
    private final String type;
    private List<FieldData> nestedTypes = null;
    private String value;

    public FieldData(Long id, String name, String type, String value) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public void setNestedTypes(List<FieldData> nestedTypes) {
        this.nestedTypes = nestedTypes;
    }

    public Long getID() {
        return id;
    }

    public void setValue(String newValue) {
        value = newValue;
    }
}
