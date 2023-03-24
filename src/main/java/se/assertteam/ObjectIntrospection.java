package se.assertteam;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ObjectIntrospection {

    public static int executionDepth = 3;

    private final ObjectGraph objectGraph;

    public ObjectIntrospection() {
        this.objectGraph = new ObjectGraph();
    }

    public static void setExecutionDepth(int executionDepth) {
        ObjectIntrospection.executionDepth = executionDepth;
    }

    public RuntimeValue introspect(LocalVariable variable) throws IllegalAccessException {
        return introspect(
                RuntimeValue.Kind.LOCAL_VARIABLE,
                variable.getName(),
                variable.getType(),
                variable.getValue(),
                0);
    }

    public List<RuntimeValue> introspectReceiver(Object receiver) throws IllegalAccessException {
        return introspectFields(receiver, 0);
    }

    public RuntimeReturnedValue introspectReturnValue(
            String methodName,
            Object returned,
            List<Object> parameters,
            List<String> stacktrace,
            String location)
            throws IllegalAccessException {
        List<RuntimeValue> fields = introspectFields(returned, 0);
        List<Object> arrayValues = introspectArrayValues(returned, 0);

        return new RuntimeReturnedValue(
                // FIXME: Use method return type
                RuntimeValue.Kind.RETURN,
                methodName,
                returned == null ? null : returned.getClass(),
                Objects.toString(returned),
                fields,
                arrayValues,
                parameters,
                stacktrace,
                location);
    }

    private RuntimeValue introspect(
            RuntimeValue.Kind kind, String name, Class<?> type, Object object, int depth)
            throws IllegalAccessException {
        List<RuntimeValue> fields = introspectFields(object, depth);
        List<Object> arrayElements = introspectArrayValues(object, depth);

        return new RuntimeValue(kind, name, type, Objects.toString(object), fields, arrayElements);
    }

    private List<RuntimeValue> introspectFields(Object object, int depth)
            throws IllegalAccessException {
        if (object == null) {
            return List.of();
        }
        if (depth > executionDepth || Classes.isBasicallyPrimitive(object.getClass())) {
            return List.of();
        }

        ObjectGraph.ObjectNode node = objectGraph.getNode(object.getClass());
        List<RuntimeValue> fields = new ArrayList<>();

        for (Field field : node.getFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            if (field.getName().startsWith("CGLIB$")) {
                continue;
            }
            if (!field.trySetAccessible()) {
                throw new AssertionError("Could not crack type " + field.getDeclaringClass());
            }
            fields.add(
                    introspect(
                            RuntimeValue.Kind.FIELD,
                            field.getName(),
                            field.getType(),
                            field.get(object),
                            depth + 1));
        }

        return fields;
    }

    private List<Object> introspectArrayValues(Object array, int depth)
            throws IllegalAccessException {
        if (array == null || !array.getClass().isArray()) {
            return List.of();
        }

        List<Object> arrayElements = new ArrayList<>();
        Class<?> componentType = array.getClass().getComponentType();

        for (int i = 0; i < Array.getLength(array) && i < 10; i++) {
            if (Classes.isBasicallyPrimitive(componentType)) {
                arrayElements.add(Array.get(array, i));
            } else {
                arrayElements.add(
                        introspect(
                                RuntimeValue.Kind.ARRAY_ELEMENT,
                                null,
                                componentType,
                                Array.get(array, i),
                                depth + 1));
            }
        }

        return arrayElements;
    }
}
