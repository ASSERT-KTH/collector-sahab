package se.assertteam.util;

import java.lang.reflect.Array;
import java.util.HashMap;
import se.assertteam.runtime.RuntimeValue;

public class Classes {

    private static final HashMap<Class<?>, Boolean> CACHED_CLASSES = new HashMap<>();

    static {
        CACHED_CLASSES.put(Object.class, Boolean.FALSE);
    }

    public static String getCanonicalClassName(Class<?> type) {
        String canonicalName = type.getCanonicalName();
        if (canonicalName != null) {
            // Ensure `$` is used as separator, even if the canonical name actually contains a `.`
            if (type.getDeclaringClass() != null) {
                return getCanonicalClassName(type.getDeclaringClass()) + "$" + type.getSimpleName();
            }
            // and fix the same thing for arrays if inner types...
            if (type.isArray()) {
                return getCanonicalClassName(type.getComponentType()) + "[]";
            }
            return canonicalName;
        }
        return type.getName();
    }

    public static boolean isBasicallyPrimitive(Class<?> clazz) {
        return clazz.isPrimitive() || clazz == String.class || isBoxed(clazz);
    }

    public static Class<?> getType(Object returned, Class<?> returnType) {
        if (returnType.isPrimitive()) {
            return returnType;
        }
        return returned == null ? returnType : returned.getClass();
    }

    private static boolean isBoxed(Class<?> clazz) {
        return clazz == Byte.class
                || clazz == Short.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Float.class
                || clazz == Double.class
                || clazz == Boolean.class
                || clazz == Character.class;
    }

    public static Class<?> getClassFromString(String className) {
        switch (className) {
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
            default:
                try {
                    if (className.endsWith("[]")) {
                        int indexOfFirstBracket = className.indexOf('[');
                        int dimensions = (className.length() - indexOfFirstBracket) / 2;
                        return getArrayClass(
                                getClassFromString(className.substring(0, indexOfFirstBracket)), dimensions);
                    }
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
        }
    }

    public static Class<?> getArrayClass(Class<?> componentType, int dimensions) {
        if (dimensions == 1) {
            return Array.newInstance(componentType, 0).getClass();
        }
        return getArrayClass(Array.newInstance(componentType, 0).getClass(), dimensions - 1);
    }

    public static boolean isArrayBasicallyPrimitive(Object value) {
        if (value == null || !value.getClass().isArray()) {
            return false;
        }
        for (int i = 0; i < Array.getLength(value); i++) {
            Object arrayEntry = Array.get(value, i);
            if (arrayEntry != null && !isBasicallyPrimitive(arrayEntry.getClass())) {
                return false;
            }
        }
        return true;
    }

    public static Object simplifyValue(RuntimeValue runtimeValue) {
        Object value = runtimeValue.getValue();
        if (value == null) {
            return null;
        }
        if (isBasicallyPrimitive(value.getClass())) {
            if (value instanceof Number || value instanceof Boolean) {
                return value;
            }
            return value.toString();
        }
        if (hasImplementedToString(value.getClass())) {
            return value.toString();
        }
        return getCanonicalClassName(value.getClass());
    }

    /**
     * Returns whether the type declares `toString`.
     */
    public static boolean hasImplementedToString(Class<?> type) {
        try {
            if (type == null) {
                return false;
            }
            if (!CACHED_CLASSES.containsKey(type)) {
                CACHED_CLASSES.put(type, type.getMethod("toString").getDeclaringClass() != Object.class);
            }

            return CACHED_CLASSES.get(type);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Unreachable", e);
        }
    }
}
