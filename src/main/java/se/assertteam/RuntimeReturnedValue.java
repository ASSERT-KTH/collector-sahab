package se.assertteam;

import java.util.List;

public class RuntimeReturnedValue extends RuntimeValue {

    private final List<Object> arguments;
    private final List<String> stackTrace;

    private final String location;

    RuntimeReturnedValue(
            Kind kind,
            String name,
            Class<?> type,
            String valueAsString,
            List<RuntimeValue> fields,
            List<RuntimeValue> arrayElements,
            List<Object> parameters,
            List<String> stackTrace,
            String location) {
        super(kind, name, type, valueAsString, fields, arrayElements);

        this.arguments = parameters;
        this.stackTrace = stackTrace;
        this.location = location;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public List<String> getStackTrace() {
        return stackTrace;
    }

    public String getLocation() {
        return location;
    }
}
