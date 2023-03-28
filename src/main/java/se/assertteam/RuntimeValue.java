package se.assertteam;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RuntimeValue {

    private final Kind kind;
    private final String name;
    private final Class<?> type;
    private final Object value;
    private final List<RuntimeValue> fields;
    private final List<RuntimeValue> arrayElements;

    RuntimeValue(
            @JsonProperty("kind") Kind kind,
            @JsonProperty("name") String name,
            @JsonProperty("type") Class<?> type,
            @JsonProperty("value") Object value,
            @JsonProperty("fields") List<RuntimeValue> fields,
            @JsonProperty("arrayElements") List<RuntimeValue> arrayElements) {
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

    public List<RuntimeValue> getArrayElements() {
        return arrayElements;
    }

    public enum Kind {
        FIELD,
        LOCAL_VARIABLE,
        RETURN,
        ARRAY_ELEMENT,
    }
}
