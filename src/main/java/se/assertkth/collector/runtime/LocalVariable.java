package se.assertkth.collector.runtime;

import java.util.Objects;

public class LocalVariable {
    private final String name;
    private final Class<?> type;
    private final Object value;

    public LocalVariable(String name, Class<?> type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocalVariable that = (LocalVariable) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, value);
    }
}
