package se.kth.debug.struct;

public class MethodForExitEvent {
    private final String name;
    private final String className;

    public MethodForExitEvent(String name, String className) {
        this.name = name;
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }
}
