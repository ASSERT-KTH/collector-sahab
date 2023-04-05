package se.assertteam.util;

import static se.assertteam.util.Classes.getCanonicalClassName;
import static se.assertteam.util.Classes.getType;
import static se.assertteam.util.Classes.isBasicallyPrimitive;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import se.assertteam.CollectorAgent;
import se.assertteam.module.ModuleCracker;
import se.assertteam.runtime.LocalVariable;
import se.assertteam.runtime.RuntimeReturnedValue;
import se.assertteam.runtime.RuntimeValue;

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

    /**
     * Introspects a local variable of a method.
     *
     * @param variable the variable to introspect
     * @return the created {@link RuntimeValue}
     * @throws IllegalAccessException if a field could not be read
     */
    public RuntimeValue introspectVariable(LocalVariable variable) throws IllegalAccessException {
        return introspectObject(
                RuntimeValue.Kind.LOCAL_VARIABLE,
                variable.getName(),
                getType(variable.getValue(), variable.getType()),
                variable.getValue(),
                // Depth is 1 if we go inside a variable
                1);
    }

    /**
     * Introspects the fields of the {@code this} object in a method.
     *
     * @param receiver the receiver
     * @param receiverClass the class of the receiver, necessary for static methods without a
     *     receiver
     * @return all field values for the receiver
     * @throws IllegalAccessException if a field could not be read
     */
    public List<RuntimeValue> introspectReceiverFields(Object receiver, Class<?> receiverClass)
            throws IllegalAccessException {
        // We do not use introspectObject here, as we do not write arrayElements for the receiver
        return getFieldValues(receiver, 0, receiverClass);
    }

    /**
     * Introspects the return value of a method.
     *
     * @param methodName the name of the method
     * @param returned the returned value
     * @param parameters the parameters of the method
     * @param stacktrace the stacktrace
     * @param location the location of the return instruction
     * @param receiverClass the receiver class
     * @param returnType the type of the return value
     * @return the created {@link RuntimeReturnedValue}
     * @throws IllegalAccessException if a field could not be read
     */
    public RuntimeReturnedValue introspectReturnValue(
            String methodName,
            Object returned,
            List<RuntimeValue> parameters,
            List<String> stacktrace,
            String location,
            Class<?> receiverClass,
            Class<?> returnType)
            throws IllegalAccessException {
        // We gather the static fields of the receiver class
        List<RuntimeValue> fields = getFieldValues(returned, 1, receiverClass);
        List<RuntimeValue> arrayValues = getArrayValues(returned, 1);

        return new RuntimeReturnedValue(
                RuntimeValue.Kind.RETURN,
                methodName,
                Classes.getCanonicalClassName(getType(returned, returnType)),
                returned,
                fields,
                arrayValues,
                parameters,
                stacktrace,
                location);
    }

    /**
     * Introspects an object, creating a {@link RuntimeValue}.
     *
     * @param kind the kind of the value
     * @param name the name of this value
     * @param type the (dynamic or static) type of this value
     * @param object the object itself
     * @param depth the current depth
     * @return the created runtime value
     * @throws IllegalAccessException if a field could not be read
     */
    private RuntimeValue introspectObject(RuntimeValue.Kind kind, String name, Class<?> type, Object object, int depth)
            throws IllegalAccessException {
        List<RuntimeValue> fields = List.of();
        List<RuntimeValue> arrayElements = List.of();

        // Depth 0 means we are at the top level, so we do not want to introspect
        // However, declared fields will be recorded at depth 0 as well
        if (depth <= executionDepth) {
            fields = getFieldValues(object, depth, type);
            arrayElements = getArrayValues(object, depth);
        }

        return new RuntimeValue(kind, name, getCanonicalClassName(type), object, fields, arrayElements);
    }

    /**
     * Introspects an object and, if it not
     * {@link Classes#isBasicallyPrimitive(Class) basically primitive}, return the field values.
     *
     * @param object the object to inspect
     * @param depth the current depth
     * @param objectStaticType the static type of the object. Used of the object is {@code null}
     *   to at least extract static fields.
     * @return the fields or an empty list if it was
     *   {@link Classes#isBasicallyPrimitive(Class) basically primitive}
     * @throws IllegalAccessException if a field could not be read
     */
    private List<RuntimeValue> getFieldValues(Object object, int depth, Class<?> objectStaticType)
            throws IllegalAccessException {
        Class<?> objectResolvedType = object != null ? object.getClass() : objectStaticType;

        if (isBasicallyPrimitive(objectResolvedType)) {
            // primitives and strings need no fields
            return List.of();
        }

        List<RuntimeValue> fieldValues = new ArrayList<>();
        // If we are in a static method or serializing null values, we are only interested in
        // static field values.
        boolean onlyCollectStaticFields = object == null;

        for (Field field : objectGraph.getNode(objectResolvedType).getFields()) {
            if (onlyCollectStaticFields && !Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.isSynthetic()) {
                continue;
            }
            if (field.getName().startsWith("CGLIB$")) {
                continue;
            }
            if (!field.trySetAccessible()) {
                moduleCracker.crack(field.getDeclaringClass());
                moduleCracker.crack(field.getType());
            }
            if (!field.trySetAccessible()) {
                throw new AssertionError("Could not crack type " + field.getDeclaringClass());
            }
            Object value = field.get(object);
            fieldValues.add(introspectObject(
                    RuntimeValue.Kind.FIELD, field.getName(), getType(value, field.getType()), value, depth + 1));
        }
        return fieldValues;
    }

    /**
     * Introspects an object and, if it is an array, returns the introspected values.
     *
     * @param array the potential array
     * @param depth the current depth
     * @return the array entries or an empty list if it has none or is nm array
     * @throws IllegalAccessException if a field could not be read
     */
    private List<RuntimeValue> getArrayValues(Object array, int depth) throws IllegalAccessException {
        if (array == null || !array.getClass().isArray()) {
            return List.of();
        }

        List<RuntimeValue> arrayElements = new ArrayList<>();
        Class<?> componentType = array.getClass().getComponentType();

        for (int i = 0; i < Array.getLength(array) && i < 20; i++) {
            Object value = Array.get(array, i);
            arrayElements.add(introspectObject(
                    RuntimeValue.Kind.ARRAY_ELEMENT, null, getType(value, componentType), value, depth + 1));
        }

        return arrayElements;
    }
}
