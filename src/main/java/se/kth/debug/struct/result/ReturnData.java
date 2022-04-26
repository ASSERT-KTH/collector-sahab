package se.kth.debug.struct.result;

import java.util.List;

public class ReturnData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.RETURN;
    private final String methodName;
    private final String type;
    private final Object value;
    private final String location;
    private final List<LocalVariableData> arguments;
    private final List<String> stackTrace;
    private List<FieldData> nestedObjects = null;

    public ReturnData(
            String methodName,
            String type,
            Object value,
            String location,
            List<LocalVariableData> arguments,
            List<String> stackTrace) {
        this.methodName = methodName;
        this.type = type;
        this.value = value;
        this.location = location;
        this.arguments = arguments;
        this.stackTrace = stackTrace;
    }

    public void setNestedObjects(List<FieldData> nestedObjects) {
        this.nestedObjects = nestedObjects;
    }

    @Override
    public RuntimeValueKind getKind() {
        return kind;
    }

    @Override
    public Object getValue() {
        return value;
    }
}
