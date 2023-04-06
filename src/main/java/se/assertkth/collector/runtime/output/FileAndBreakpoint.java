package se.assertkth.collector.runtime.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.List;

public class FileAndBreakpoint {

    private final String fileName;

    private final List<Integer> breakpoints;

    public FileAndBreakpoint(
            @JsonProperty("fileName") String fileName, @JsonProperty("breakpoints") List<Integer> breakpoints) {
        this.fileName = fileName;
        this.breakpoints = breakpoints;
    }

    public String getFileName() {
        return fileName;
    }

    public List<Integer> getBreakpoints() {
        return breakpoints;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FileAndBreakpoint other = (FileAndBreakpoint) obj;
        return fileName.equals(other.fileName) && new HashSet<>(breakpoints).equals(new HashSet<>(other.breakpoints));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + fileName.hashCode();
        result = prime * result + breakpoints.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "[" + fileName + ":" + breakpoints + "]";
    }
}
