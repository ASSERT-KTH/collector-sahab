package se.kth.debug.struct.result;

public interface RuntimeValue {
    RuntimeValueKind getKind();

    Long getID();

    String getValue();

    void setValue(String value);
}
