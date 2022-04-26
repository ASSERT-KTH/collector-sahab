package se.kth.debug.struct.result;

public interface RuntimeValue {
    RuntimeValueKind getKind();

    Object getValue();
}
