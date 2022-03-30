package se.kth.debug.struct.result;

import java.util.List;

public class ReturnData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.RETURN;
    private final String methodName;
    private final String returnType;
    private final String value;
    private final String location;
    private final List<LocalVariableData> arguments;
    private final List<String> stackTrace;
    private List<FieldData> nestedTypes = null;

    public ReturnData(
            String methodName,
            String returnType,
            String value,
            String location,
            List<LocalVariableData> arguments,
            List<String> stackTrace) {
        this.methodName = methodName;
        this.returnType = returnType;
        this.value = value;
        this.location = location;
        this.arguments = arguments;
        this.stackTrace = stackTrace;
    }

    public void setNestedTypes(List<FieldData> nestedTypes) {
        this.nestedTypes = nestedTypes;
    }
}
