package se.kth.debug.struct.result;

import java.util.List;

public class LocalVariableData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.LOCAL_VARIABLE;
    private final String name;
    private final ValueWrapper value;
    private List<FieldData> fields = null;

    public LocalVariableData(String name, ValueWrapper value) {
        this.name = name;
        this.value = value;
    }

    public void setFields(List<FieldData> fields) {
        this.fields = fields;
    }

    @Override
    public RuntimeValueKind getKind() {
        return kind;
    }

    @Override
    public ValueWrapper getValueWrapper() {
        return value;
    }
}
