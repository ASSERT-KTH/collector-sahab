package se.kth.debug.struct.result;

import java.util.List;

public interface Statistics<T> {
    void addData(String name, String value);

    String getIdentifier();

    List<T> getCollection();
}
