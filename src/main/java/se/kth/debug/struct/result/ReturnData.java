package se.kth.debug.struct.result;

import java.util.List;

public class ReturnData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.RETURN;
    private final String methodName;
    private final String value;
    private final String location;
    private List<FieldData> nestedTypes = null;

    public ReturnData(String methodName, String value, String location) {
        this.methodName = methodName;
        this.value = value;
        this.location = location;
    }

    public void setNestedTypes(List<FieldData> nestedTypes) {
        this.nestedTypes = nestedTypes;
    }
}
