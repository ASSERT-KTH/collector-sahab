package se.kth.debug.struct.result.localVariable;

import se.kth.debug.struct.result.RuntimeValueCollection;

public interface LocalVariableList extends RuntimeValueCollection {
    void addData(String name, String value);
}
