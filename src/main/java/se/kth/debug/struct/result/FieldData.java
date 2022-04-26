package se.kth.debug.struct.result;

import java.util.List;

public class FieldData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.FIELD;
    private final String name;
    private final ValueWrapper value;
    private List<FieldData> fields = null;

    public FieldData(String name, ValueWrapper value) {
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
