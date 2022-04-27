package se.kth.debug.struct.result;

import java.util.List;

public interface RuntimeValue {
    RuntimeValueKind getKind();

    ValueWrapper getValueWrapper();

    List<FieldData> getFields();
}
