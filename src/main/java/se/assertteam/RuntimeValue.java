package se.assertteam;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.List;
import se.assertteam.RuntimeValue.RuntimeValueSerializer;

@JsonSerialize(using = RuntimeValueSerializer.class)
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

    static class RuntimeValueSerializer extends JsonSerializer<RuntimeValue> {

        @Override
        public void serialize(RuntimeValue value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
            if (value.kind == Kind.ARRAY_ELEMENT && Classes.isBasicallyPrimitive(value.type)) {
                // Value is enough for values in arrays
                serializers.defaultSerializeValue(value.value, gen);
                return;
            }
            gen.writeStartObject();
            serializers.defaultSerializeField("kind", value.kind, gen);
            serializers.defaultSerializeField("name", value.name, gen);
            serializers.defaultSerializeField("type", value.type, gen);
            serializers.defaultSerializeField("value", value.value, gen);
            serializers.defaultSerializeField("fields", value.fields, gen);
            serializers.defaultSerializeField("arrayElements", value.arrayElements, gen);
            gen.writeEndObject();
        }
    }

}
