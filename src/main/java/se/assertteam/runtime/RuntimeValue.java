package se.assertteam.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import se.assertteam.runtime.RuntimeValue.RuntimeValueSerializer;
import se.assertteam.util.Classes;

@JsonSerialize(using = RuntimeValueSerializer.class)
public class RuntimeValue {

    private final Kind kind;
    private final String name;
    private final String type;
    private final Object value;
    private final List<RuntimeValue> fields;
    private final List<RuntimeValue> arrayElements;

    protected RuntimeValue(
            @JsonProperty("kind") Kind kind,
            @JsonProperty("name") String name,
            @JsonProperty("type") String type,
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

    public String getType() {
        return type;
    }

    public List<RuntimeValue> getFields() {
        return fields;
    }

    public List<RuntimeValue> getArrayElements() {
        return arrayElements;
    }

    public static RuntimeValue fromObservation(
            Kind kind,
            String name,
            String type,
            Object value,
            List<RuntimeValue> fields,
            List<RuntimeValue> arrayElements) {
        if (Classes.isArrayBasicallyPrimitive(value)) {
            return new RuntimeValue(kind, name, type, value, fields, List.of());
        } else {
            return new RuntimeValue(kind, name, type, Classes.simplifyValue(value), fields, arrayElements);
        }
    }

    public static RuntimeValue fromRaw(
            Kind kind,
            String name,
            String type,
            Object value,
            List<RuntimeValue> fields,
            List<RuntimeValue> arrayElements) {
        return new RuntimeValue(kind, name, type, value, fields, arrayElements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RuntimeValue that = (RuntimeValue) o;
        return kind == that.kind
                && Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && Objects.equals(value, that.value)
                && Objects.equals(fields, that.fields)
                && Objects.equals(arrayElements, that.arrayElements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, name, type, value, fields, arrayElements);
    }

    public enum Kind {
        FIELD,
        LOCAL_VARIABLE,
        RETURN,
        ARRAY_ELEMENT,
    }

    static class RuntimeValueSerializer extends JsonSerializer<RuntimeValue> {

        @Override
        public void serialize(RuntimeValue value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            serializers.defaultSerializeField("kind", value.kind, gen);
            serializers.defaultSerializeField("name", value.name, gen);
            serializers.defaultSerializeField("type", value.type, gen);
            serializers.defaultSerializeField("fields", value.fields, gen);
            serializers.defaultSerializeField("value", value.value, gen);
            serializers.defaultSerializeField("arrayElements", value.arrayElements, gen);
            gen.writeEndObject();
        }
    }
}
