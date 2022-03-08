package se.kth.debug.struct.result;

import java.util.List;

public class LocalVariableData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.LOCAL_VARIABLE;
    private final String name;
    private final String value;
    private List<FieldData> primitiveTypes = null;

    public LocalVariableData(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public void setPrimitiveTypes(List<FieldData> primitiveTypes) {
        this.primitiveTypes = primitiveTypes;
    }
}
