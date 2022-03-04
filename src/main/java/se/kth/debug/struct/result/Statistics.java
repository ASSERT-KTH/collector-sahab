package se.kth.debug.struct.result;

import java.util.List;

public interface Statistics<T extends RuntimeValue> {
    void addData(String name, String value);

    String getIdentifier();

    List<T> getCollection();
}

interface RuntimeValue { }
