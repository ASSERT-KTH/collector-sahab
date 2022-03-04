package se.kth.debug.struct.result;

import java.util.List;

public interface RuntimeValueTypeChunk<T extends RuntimeValue> {
    void addData(String name, String value);

    String getIdentifier();

    List<T> getCollection();
}
