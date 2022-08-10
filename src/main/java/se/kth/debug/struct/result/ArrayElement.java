package se.kth.debug.struct.result;

import java.util.List;

public class ArrayElement implements RuntimeValue {
    private final RuntimeValueKind kind = RuntimeValueKind.ARRAY_ELEMENT;
    private final String type;
    private Object value;
    private List<FieldData> fields = null;
    private List<ArrayElement> arrayElements = null;

    public ArrayElement(String type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public RuntimeValueKind getKind() {
        return kind;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public List<FieldData> getFields() {
        return fields;
    }

    @Override
    public List<ArrayElement> getArrayElements() {
        return arrayElements;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    public void setArrayElements(List<ArrayElement> arrayElements) {
        this.arrayElements = arrayElements;
    }

    public void setFields(List<FieldData> fields) {
        this.fields = fields;
    }
}
