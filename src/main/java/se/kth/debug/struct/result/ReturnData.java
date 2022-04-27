package se.kth.debug.struct.result;

import java.util.List;

public class ReturnData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.RETURN;
    private final String methodName;
    private final List<String> stackTrace;
    private final ValueWrapper value;
    private final String location;
    private final List<LocalVariableData> arguments;
    private List<FieldData> fields = null;

    public ReturnData(
            String methodName,
            ValueWrapper value,
            String location,
            List<LocalVariableData> arguments,
            List<String> stackTrace) {
        this.methodName = methodName;
        this.value = value;
        this.location = location;
        this.arguments = arguments;
        this.stackTrace = stackTrace;
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

    @Override
    public List<FieldData> getFields() {
        return fields;
    }
}
