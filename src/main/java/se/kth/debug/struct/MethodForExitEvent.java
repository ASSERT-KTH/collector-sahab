package se.kth.debug.struct;

public class MethodForExitEvent {
    private final String name;
    private final String signature;
    private final String className;

    public MethodForExitEvent(String name, String signature, String className) {
        this.name = name;
        this.signature = signature;
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public String getClassName() {
        return className;
    }
}
