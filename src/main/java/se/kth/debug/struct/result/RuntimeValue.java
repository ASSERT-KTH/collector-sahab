package se.kth.debug.struct.result;

public interface RuntimeValue {
    RuntimeValueKind getKind();

    Long getID();

    ValueWrapper getValueWrapper();

    void setValue(ValueWrapper value);
}
