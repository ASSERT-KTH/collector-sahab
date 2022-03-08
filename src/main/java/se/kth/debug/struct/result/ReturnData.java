package se.kth.debug.struct.result;

public class ReturnData implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.RETURN;
    private final String methodName;
    private final String value;

    public ReturnData(String methodName, String value) {
        this.methodName = methodName;
        this.value = value;
    }
}
