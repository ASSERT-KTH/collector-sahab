package se.kth.debug.struct.result;

import java.util.List;

public class ValueWrapper {
    private String type;
    private Object atomicValue;
    private List<?> nestedObjects = null;

    public ValueWrapper(String type, Object atomicValue) {
        this.type = type;
        this.atomicValue = atomicValue;
    }

    public void setNestedObjects(List<?> nestedObjects) {
        this.nestedObjects = nestedObjects;
    }

    public String getType() {
        return type;
    }

    public Object getAtomicValue() {
        return atomicValue;
    }

    public List<?> getNestedObjects() {
        return nestedObjects;
    }
}
