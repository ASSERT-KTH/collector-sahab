package se.kth.debug.struct.result;

public interface RuntimeValue {
    RuntimeValueKind getKind();

    Long getID();

    Object getValue();

    void setValue(Object value);
}
