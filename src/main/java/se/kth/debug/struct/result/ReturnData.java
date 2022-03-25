package se.kth.debug.struct.result;

public class ReturnData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.RETURN;
    private final String methodName;
    private final String value;
    private final String location;

    public ReturnData(String methodName, String value, String location) {
        this.methodName = methodName;
        this.value = value;
        this.location = location;
    }
}
