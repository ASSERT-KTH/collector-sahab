package se.kth.debug;

import com.google.gson.*;
import java.lang.reflect.Type;
import se.kth.debug.struct.result.ReturnData;

public class ReturnDataAdapter implements JsonSerializer<ReturnData> {
    @Override
    public JsonElement serialize(
            ReturnData returnData, Type type, JsonSerializationContext context) {
        if (returnData == null) {
            return JsonNull.INSTANCE;
        }
        JsonObject object = new JsonObject();

        object.add("kind", context.serialize(returnData.getKind()));
        object.add("methodName", context.serialize(returnData.getMethodName()));
        object.add("stackTrace", context.serialize(returnData.getStackTrace()));
        object.add("type", context.serialize(returnData.getType()));
        object.add("location", context.serialize(returnData.getLocation()));
        object.add("parameterValues", context.serialize(returnData.getParameterValues()));
        object.add("fields", context.serialize(returnData.getFields()));
        object.add("arrayElements", context.serialize(returnData.getArrayElements()));

        if (returnData.getValue() != null
                && (returnData.getValue().equals(Double.POSITIVE_INFINITY)
                        || returnData.getValue().equals(Double.NEGATIVE_INFINITY)
                        || returnData.getValue().equals(Double.NaN))) {
            returnData.setValue(returnData.getValue().toString());
        }
        object.add("value", context.serialize(returnData.getValue()));
        return object;
    }
}
