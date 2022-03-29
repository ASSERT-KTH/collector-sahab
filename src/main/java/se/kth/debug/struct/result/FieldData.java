package se.kth.debug.struct.result;

import java.util.List;

public class FieldData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.FIELD;
    private final String name;
    private final String type;
    private final String value;
    private List<FieldData> nestedTypes = null;

    public FieldData(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public void setNestedTypes(List<FieldData> nestedTypes) {
        this.nestedTypes = nestedTypes;
    }
}
