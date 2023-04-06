package se.assertkth.collector.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectGraph {

    private final Map<Class<?>, ObjectNode> nodeGraph;

    public ObjectGraph() {
        this.nodeGraph = new ConcurrentHashMap<>();
    }

    public ObjectNode getNode(Class<?> clazz) {
        return nodeGraph.computeIfAbsent(clazz, ObjectNode::forClass);
    }

    public static class ObjectNode {

        private final Class<?> clazz;
        private final List<Field> fields;

        private ObjectNode(Class<?> clazz, List<Field> fields) {
            this.clazz = clazz;
            this.fields = List.copyOf(fields);
        }

        public List<Field> getFields() {
            return fields;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        private static ObjectNode forClass(Class<?> clazz) {
            return new ObjectNode(clazz, buildFieldsRecursively(clazz));
        }

        private static List<Field> buildFieldsRecursively(Class<?> current) {
            List<Field> fields = new ArrayList<>();
            buildFieldsRecursively(current, fields);
            return fields;
        }

        private static void buildFieldsRecursively(Class<?> current, List<Field> output) {
            output.addAll(List.of(current.getDeclaredFields()));
            if (current.getSuperclass() != null) {
                buildFieldsRecursively(current.getSuperclass(), output);
            }
        }
    }
}
