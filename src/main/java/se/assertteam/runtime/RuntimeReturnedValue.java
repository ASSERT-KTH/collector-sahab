package se.assertteam.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.List;
import se.assertteam.util.Classes;

@JsonSerialize(using = RuntimeReturnedValue.RuntimeReturnedValueSerializer.class)
public class RuntimeReturnedValue extends RuntimeValue {

    private final List<RuntimeValue> arguments;
    private final List<String> stackTrace;

    private final String location;

    public RuntimeReturnedValue(
            @JsonProperty("kind") Kind kind,
            @JsonProperty("methodName") String name,
            @JsonProperty("type") String type,
            @JsonProperty("value") Object value,
            @JsonProperty("fields") List<RuntimeValue> fields,
            @JsonProperty("arrayElements") List<RuntimeValue> arrayElements,
            @JsonProperty("parameterValues") List<RuntimeValue> parameters,
            @JsonProperty("stackTrace") List<String> stackTrace,
            @JsonProperty("location") String location) {
        super(kind, name, type, value, fields, arrayElements);

        this.arguments = parameters;
        this.stackTrace = stackTrace;
        this.location = location;
    }

    public List<RuntimeValue> getArguments() {
        return arguments;
    }

    public List<String> getStackTrace() {
        return stackTrace;
    }

    public String getLocation() {
        return location;
    }

    static class RuntimeReturnedValueSerializer extends JsonSerializer<RuntimeReturnedValue> {

        @Override
        public void serialize(RuntimeReturnedValue value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            serializers.defaultSerializeField("kind", value.getKind(), gen);
            serializers.defaultSerializeField("methodName", value.getName(), gen);
            serializers.defaultSerializeField("stackTrace", value.getStackTrace(), gen);
            serializers.defaultSerializeField("type", value.getType(), gen);
            if (Classes.isArrayBasicallyPrimitive(value.getValue())) {
                serializers.defaultSerializeField("arrayElements", List.of(), gen);
                serializers.defaultSerializeField("value", value.getValue(), gen);
            } else {
                serializers.defaultSerializeField("value", Classes.simplifyValue(value), gen);
                serializers.defaultSerializeField("arrayElements", value.getArrayElements(), gen);
            }
            serializers.defaultSerializeField("fields", value.getFields(), gen);
            serializers.defaultSerializeField("location", value.getLocation(), gen);
            serializers.defaultSerializeField("parameterValues", value.getArguments(), gen);
            gen.writeEndObject();
        }
    }
}
