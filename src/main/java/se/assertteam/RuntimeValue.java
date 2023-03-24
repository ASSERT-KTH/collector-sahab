package se.assertteam;

import java.util.List;

public class RuntimeValue {

    private final Kind kind;
    private final String name;
    private final Class<?> type;
    private final String value;
    private final List<RuntimeValue> fields;
    private final List<Object> arrayElements;

    RuntimeValue(
            Kind kind,
            String name,
            Class<?> type,
            String value,
            List<RuntimeValue> fields,
            List<Object> arrayElements) {
        this.kind = kind;
        this.name = name;
        this.type = type;
        this.value = value;
        this.fields = fields;
        this.arrayElements = arrayElements;
    }

    public Kind getKind() {
        return kind;
    }

    public Object getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public List<RuntimeValue> getFields() {
        return fields;
    }

    public List<Object> getArrayElements() {
        return arrayElements;
    }

    public enum Kind {
        FIELD,
        LOCAL_VARIABLE,
        RETURN,
        ARRAY_ELEMENT,
    }
}
