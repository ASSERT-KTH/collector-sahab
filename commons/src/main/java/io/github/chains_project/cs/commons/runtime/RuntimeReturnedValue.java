package io.github.chains_project.cs.commons.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.chains_project.cs.commons.util.Classes;
import java.io.IOException;
import java.util.List;

@JsonSerialize(using = RuntimeReturnedValue.RuntimeReturnedValueSerializer.class)
public class RuntimeReturnedValue extends RuntimeValue {

    private final List<RuntimeValue> arguments;
    private final List<String> stackTrace;

    private final String location;

    protected RuntimeReturnedValue(
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

    public static RuntimeReturnedValue fromObservation(
            @JsonProperty("kind") Kind kind,
            @JsonProperty("methodName") String name,
            @JsonProperty("type") String type,
            @JsonProperty("value") Object value,
            @JsonProperty("fields") List<RuntimeValue> fields,
            @JsonProperty("arrayElements") List<RuntimeValue> arrayElements,
            @JsonProperty("parameterValues") List<RuntimeValue> parameters,
            @JsonProperty("stackTrace") List<String> stackTrace,
            @JsonProperty("location") String location) {
        if (Classes.isArrayBasicallyPrimitive(value)) {
            // I am not sure why this is necessary, but just to be safe...
            // you cannot mutate the array after it has been returned.
            Object clonedArray = Classes.cloneArray(value);
            return new RuntimeReturnedValue(
                    kind, name, type, clonedArray, fields, List.of(), parameters, stackTrace, location);
        } else {
            return new RuntimeReturnedValue(
                    kind,
                    name,
                    type,
                    Classes.simplifyValue(value),
                    fields,
                    arrayElements,
                    parameters,
                    stackTrace,
                    location);
        }
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
            serializers.defaultSerializeField("value", value.getValue(), gen);
            serializers.defaultSerializeField("arrayElements", value.getArrayElements(), gen);
            serializers.defaultSerializeField("fields", value.getFields(), gen);
            serializers.defaultSerializeField("location", value.getLocation(), gen);
            serializers.defaultSerializeField("parameterValues", value.getArguments(), gen);
            gen.writeEndObject();
        }
    }
}
