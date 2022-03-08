package se.kth.debug;

import com.google.gson.*;
import se.kth.debug.struct.result.RuntimeValue;

import java.lang.reflect.Type;
import java.util.List;

public class RuntimeValueAdapter implements JsonSerializer<List<? extends RuntimeValue>> {
    @Override
    public JsonElement serialize(List<? extends RuntimeValue> runtimeValues, Type type, JsonSerializationContext context) {
        if (runtimeValues == null) {
            return JsonNull.INSTANCE;
        }
        JsonArray result = new JsonArray();
        for (var runtimeValue: runtimeValues) {
            JsonElement jsonElement = context.serialize(runtimeValue);
            result.add(jsonElement);
        }
        return result;
    }
}
