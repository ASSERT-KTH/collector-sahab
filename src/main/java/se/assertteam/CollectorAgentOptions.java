package se.assertteam;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import se.kth.debug.struct.FileAndBreakpoint;
import se.kth.debug.struct.MethodForExitEvent;

public class CollectorAgentOptions {

    private File classesAndBreakpoints;

    private File methodsForExitEvent;

    private int numberOfArrayElements = 10;
    private File output = new File("target/output.json");
    private int executionDepth = 0;

    public CollectorAgentOptions(String javaAgentArgs) {
        if (javaAgentArgs == null || javaAgentArgs.isEmpty()) {
            return;
        }
        String[] args = javaAgentArgs.split(",");
        for (String arg : args) {
            String[] split = arg.split("=");
            if (split.length != 2) {
                throw new IllegalArgumentException("Invalid argument: " + arg);
            }
            String key = split[0];
            String value = split[1];

            switch (key) {
                case "output":
                    output = new File(value);
                    break;
                case "executionDepth":
                    executionDepth = Integer.parseInt(value);
                    break;
                case "numberOfArrayElements":
                    numberOfArrayElements = Integer.parseInt(value);
                    break;
                case "classesAndBreakpoints":
                    classesAndBreakpoints = new File(value);
                    break;
                case "methodsForExitEvent":
                    methodsForExitEvent = new File(value);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + key);
            }
        }
    }

    public File getOutput() {
        return output;
    }

    public int getExecutionDepth() {
        return executionDepth;
    }

    public int getNumberOfArrayElements() {
        return numberOfArrayElements;
    }

    public List<FileAndBreakpoint> getClassesAndBreakpoints() {
        if (classesAndBreakpoints == null) {
            throw new IllegalStateException("classesAndBreakpoints is not set");
        }
        return parseFileAndBreakpoints(classesAndBreakpoints);
    }

    private List<FileAndBreakpoint> parseFileAndBreakpoints(File classesAndBreakpoints) {

        try {

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(classesAndBreakpoints, new TypeReference<>() {});
        }  catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<MethodForExitEvent> getMethodsForExitEvent() {
        if (methodsForExitEvent == null) {
            throw new IllegalStateException("methodsForExitEvent is not set");
        }
        return parseMethodsForExitEvent(methodsForExitEvent);
    }

    private List<MethodForExitEvent> parseMethodsForExitEvent(File methodsForExitEvent) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(methodsForExitEvent, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
