package se.kth.debug.struct.result.field;

import se.kth.debug.struct.result.RuntimeValueCollection;

public interface FieldList extends RuntimeValueCollection {
    void addData(String name, String value);
}
