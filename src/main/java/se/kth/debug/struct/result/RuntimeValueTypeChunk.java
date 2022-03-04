package se.kth.debug.struct.result;

import java.util.List;

public interface RuntimeValueTypeChunk<T extends RuntimeValue> {
    void addData(String name, String value);

    void addData(FieldList fieldList);

    String getIdentifier();

    List<Object> getCollection();
}
