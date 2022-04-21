package se.kth.debug.struct.result;

import java.util.List;

public class ReturnData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.RETURN;
    private final String methodName;
    private final String returnType;
    private final String location;
    private final List<LocalVariableData> arguments;
    private final List<String> stackTrace;
    private final transient Long id;
    private List<FieldData> nestedObjects = null;
    private Object value;

    public ReturnData(
            Long id,
            String methodName,
            String returnType,
            Object value,
            String location,
            List<LocalVariableData> arguments,
            List<String> stackTrace) {
        this.id = id;
        this.methodName = methodName;
        this.returnType = returnType;
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

    public Long getID() {
        return id;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object newValue) {
        value = newValue;
    }
}
