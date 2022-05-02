package se.kth.debug.struct.result;

import java.util.List;

public interface RuntimeValue {
    RuntimeValueKind getKind();

    Object getValue();

    List<FieldData> getFields();

    List<ArrayElement> getArrayElements();

    String getName();
}
