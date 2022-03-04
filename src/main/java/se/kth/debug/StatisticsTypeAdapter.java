package se.kth.debug;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import se.kth.debug.struct.result.RuntimeValue;
import se.kth.debug.struct.result.RuntimeValueTypeChunk;

public class StatisticsTypeAdapter
        implements JsonSerializer<RuntimeValueTypeChunk<? extends RuntimeValue>> {

    @Override
    public JsonElement serialize(
            RuntimeValueTypeChunk<? extends RuntimeValue> runtimeValueTypeChunk,
            Type type,
            JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.add(
                runtimeValueTypeChunk.getIdentifier(),
                context.serialize(runtimeValueTypeChunk.getCollection(), ArrayList.class));
        return result;
    }
}
