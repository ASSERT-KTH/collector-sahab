package se.kth.debug.struct;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import spoon.reflect.declaration.CtType;

public class DebugeeType {
    final CtType<?> type;
    final Set<Integer> diffLines = new HashSet<>();
    final Set<Integer> matchedLines = new HashSet<>();

    public DebugeeType(CtType<?> type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DebugeeType)) {
            return false;
        }
        DebugeeType other = (DebugeeType) obj;
        return this.type.equals(other.type);
    }

    public Set<Integer> getDiffLines() {
        return diffLines;
    }

    public Set<Integer> getMatchedLines() {
        return matchedLines;
    }

    public CtType<?> getType() {
        return type;
    }
}
