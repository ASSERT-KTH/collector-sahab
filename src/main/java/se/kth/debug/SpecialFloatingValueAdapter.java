/**
 * I have no idea how this works, but the tests pass. I hope Khashayar never finds bug in
 * serialisation again, because I don't want to delve further into this.
 */
package se.kth.debug;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class SpecialFloatingValueAdapter implements JsonSerializer<ArrayList<?>> {

    @Override
    public JsonElement serialize(ArrayList<?> number, Type type, JsonSerializationContext context) {
        if (number == null) {
            return JsonNull.INSTANCE;
        }
        JsonArray array = new JsonArray();
        if (number.size() == 0) {
            return array;
        }
        for (Object element : number) {
            if (element.getClass().isAssignableFrom(ArrayList.class)) {
                array.add(context.serialize(element));
            } else {
                array.add(context.serialize(Utility.escapeSpecialFloatingValues(element)));
            }
        }
        return array;
    }
}
