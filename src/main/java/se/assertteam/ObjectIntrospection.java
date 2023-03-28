package se.assertteam;

import static se.assertteam.Classes.isBasicallyPrimitive;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import se.assertteam.module.ModuleCracker;

public class ObjectIntrospection {

    public static int executionDepth = 0;

    private final ObjectGraph objectGraph;

    private final ModuleCracker moduleCracker = CollectorAgent.moduleCracker;

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

    public List<RuntimeValue> introspectReceiver(Object receiver, Class<?> receiverClass)
            throws IllegalAccessException {
        // Depth is -1 because we are introspecting fields which are one level deeper than the
        // receiver
        return introspectFields(receiver, 0, receiverClass);
    }

    public RuntimeReturnedValue introspectReturnValue(
            String methodName,
            Object returned,
            List<Object> parameters,
            List<String> stacktrace,
            String location,
            Class<?> receiverClass)
            throws IllegalAccessException {
        List<RuntimeValue> fields = introspectFields(returned, 0, receiverClass);
        List<RuntimeValue> arrayValues = introspectArrayValues(returned, 0);

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
        if (depth <= executionDepth) {
            List<RuntimeValue> fields = introspectFields(object, depth, type);
            List<RuntimeValue> arrayElements = introspectArrayValues(object, depth);

            return new RuntimeValue(
                    kind, name, type, getJSONCompatibleValue(object), fields, arrayElements);
        }
        return new RuntimeValue(
                kind, name, type, getJSONCompatibleValue(object), List.of(), List.of());
    }

    private static Object getJSONCompatibleValue(Object object) {
        if (object == null) {
            return null;
        }
        if (isBasicallyPrimitive(object.getClass())) {
            if (object instanceof Number || object instanceof Boolean) return object;
            else return object.toString();
        } else {
            return object.getClass().getName();
        }
    }

    private List<RuntimeValue> introspectFields(Object object, int depth, Class<?> receiverClass)
            throws IllegalAccessException {
        if (isBasicallyPrimitive(receiverClass)) {
            return List.of();
        }
        if (object == null) {
            return traverseFields(null, depth, receiverClass);
        }

        return traverseFields(object, depth, object.getClass());
    }

    private List<RuntimeValue> traverseFields(Object object, int depth, Class<?> typeOfObject)
            throws IllegalAccessException {
        List<RuntimeValue> fields = new ArrayList<>();
        ObjectGraph.ObjectNode node = objectGraph.getNode(typeOfObject);
        for (Field field : node.getFields()) {
            if (object == null && !Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.isSynthetic()) {
                continue;
            }
            if (field.getName().startsWith("CGLIB$")) {
                continue;
            }
            if (!field.trySetAccessible()) {
                System.out.println("Cracking " + field);
                moduleCracker.crack(field.getDeclaringClass());
                moduleCracker.crack(field.getType());
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

    private List<RuntimeValue> introspectArrayValues(Object array, int depth)
            throws IllegalAccessException {
        if (array == null || !array.getClass().isArray()) {
            return List.of();
        }

        List<RuntimeValue> arrayElements = new ArrayList<>();
        Class<?> componentType = array.getClass().getComponentType();

        for (int i = 0; i < Array.getLength(array) && i < 20; i++) {
            if (isBasicallyPrimitive(componentType)) {
                arrayElements.add(
                        new RuntimeValue(
                                RuntimeValue.Kind.ARRAY_ELEMENT,
                                null,
                                componentType,
                                Array.get(array, i),
                                List.of(),
                                List.of()));
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
