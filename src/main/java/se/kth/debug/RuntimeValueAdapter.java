package se.kth.debug;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.List;
import se.kth.debug.struct.result.RuntimeValue;

public class RuntimeValueAdapter implements JsonSerializer<List<? extends RuntimeValue>> {
    @Override
    public JsonElement serialize(
            List<? extends RuntimeValue> runtimeValues,
            Type type,
            JsonSerializationContext context) {
        if (runtimeValues == null) {
            return JsonNull.INSTANCE;
        }
        JsonArray result = new JsonArray();
        for (var runtimeValue : runtimeValues) {
            if (runtimeValue != null
                    && runtimeValue.getValue() != null
                    && (runtimeValue.getValue().equals(Double.POSITIVE_INFINITY)
                            || runtimeValue.getValue().equals(Double.NEGATIVE_INFINITY)
                            || runtimeValue.getValue().equals(Double.NaN))) {
                runtimeValue.setValue(runtimeValue.getValue().toString());
            }
            JsonElement jsonElement = context.serialize(runtimeValue);
            result.add(jsonElement);
        }
        return result;
    }
}
