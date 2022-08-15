package se.kth.debug;

import com.google.gson.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import se.kth.debug.struct.result.RuntimeValue;

public class RuntimeValueAdapter implements JsonSerializer<RuntimeValue> {

    @Override
    public JsonElement serialize(
            RuntimeValue runtimeValue, Type type, JsonSerializationContext context) {
        if (runtimeValue == null) {
            return JsonNull.INSTANCE;
        }
        Field[] fields = runtimeValue.getClass().getDeclaredFields();
        JsonObject object = new JsonObject();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(runtimeValue);
                if (fieldValue != null && fieldValue.getClass().isAssignableFrom(ArrayList.class)) {
                    object.add(field.getName(), context.serialize(fieldValue));
                } else {
                    object.add(
                            field.getName(),
                            context.serialize(Utility.escapeSpecialFloatingValues(fieldValue)));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return object;
    }
}
