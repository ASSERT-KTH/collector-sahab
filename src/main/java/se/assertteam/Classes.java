package se.assertteam;

import java.lang.reflect.Array;

public class Classes {

    public static String className(Class<?> type) {
        String canonicalName = type.getCanonicalName();
        if (canonicalName != null) {
            // Ensure `$` is used as separator, even if the canonical name actually contains a `.`
            if (type.getDeclaringClass() != null) {
                return className(type.getDeclaringClass()) + "$" + type.getSimpleName();
            }
            // and fix the same thing for arrays if inner types...
            if (type.isArray()) {
                return className(type.getComponentType()) + "[]";
            }
            return canonicalName;
        }
        return type.getName();
    }

    public static boolean isBasicallyPrimitive(Class<?> clazz) {
        return clazz.isPrimitive() || clazz == String.class || isBoxed(clazz);
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

    private static Class<?> getArrayClass(Class<?> componentType, int dimensions) {
        if (dimensions == 1) {
            return Array.newInstance(componentType, 0).getClass();
        }
        return getArrayClass(Array.newInstance(componentType, 0).getClass(), dimensions - 1);
    }
}
