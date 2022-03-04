package se.kth.debug;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;

import se.kth.debug.struct.result.RuntimeValueCollection;

public class RuntimeValueCollectionTypeAdapter
        implements JsonSerializer<RuntimeValueCollection> {

    @Override
    public JsonElement serialize(
            RuntimeValueCollection runtimeValueCollection,
            Type type,
            JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.add(
                runtimeValueCollection.getIdentifier(),
                context.serialize(runtimeValueCollection.getCollection(), ArrayList.class));
        return result;
    }
}
