package se.kth.debug.struct.result;

import java.util.List;

public class LocalVariableData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.LOCAL_VARIABLE;
    private final String name;
    private final String type;
    private final String value;
    private List<FieldData> nestedTypes = null;

    public LocalVariableData(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public void setNestedTypes(List<FieldData> nestedTypes) {
        this.nestedTypes = nestedTypes;
    }
}
