package se.assertteam.runtime.output;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MethodForExitEvent {
    private final String name;
    private final String className;

    public MethodForExitEvent(@JsonProperty("name") String name, @JsonProperty("className") String className) {
        this.name = name;
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MethodForExitEvent other = (MethodForExitEvent) obj;
        return name.equals(other.name) && className.equals(other.className);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + className.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "[" + className + ":" + name + "]";
    }
}
