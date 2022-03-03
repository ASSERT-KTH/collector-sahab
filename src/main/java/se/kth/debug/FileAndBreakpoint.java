package se.kth.debug;

import java.util.List;

public class FileAndBreakpoint {
    private final String fileName;
    private final List<Integer> breakpoints;

    public FileAndBreakpoint(String fileName, List<Integer> breakpoints) {
        this.fileName = fileName;
        this.breakpoints = breakpoints;
    }

    public String getFileName() {
        return fileName;
    }

    public List<Integer> getBreakpoints() {
        return breakpoints;
    }
}
