package io.github.chains_project.cs.commons.runtime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.StackWalker.StackFrame;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StackFrameContext {

    private final int positionFromTopInStackTrace;
    private final String location;
    private final List<String> stackTrace;
    private final List<RuntimeValue> runtimeValueCollection;

    @JsonCreator
    private StackFrameContext(
            @JsonProperty("positionFromTopInStackTrace") int positionFromTopInStackTrace,
            @JsonProperty("location") String location,
            @JsonProperty("stackTrace") List<String> stackTrace,
            @JsonProperty("runtimeValueCollection") List<RuntimeValue> runtimeValueCollection) {
        this.positionFromTopInStackTrace = positionFromTopInStackTrace;
        this.location = location;
        this.stackTrace = stackTrace;
        this.runtimeValueCollection = runtimeValueCollection;
    }

    private StackFrameContext(List<StackFrame> stackTrace, List<RuntimeValue> runtimeValueCollection) {
        this.positionFromTopInStackTrace = 1;
        this.stackTrace =
                stackTrace.stream().map(StackFrameContext::stackFrameToString).collect(Collectors.toList());
        this.location = getLocation(stackTrace);
        this.runtimeValueCollection = runtimeValueCollection;
    }

    public static String getLocation(List<StackFrame> stackTrace) {
        return stackTrace.get(0).getClassName() + ":" + stackTrace.get(0).getLineNumber();
    }

    public String getLocation() {
        return location;
    }

    public int getPositionFromTopInStackTrace() {
        return positionFromTopInStackTrace;
    }

    public List<String> getStackTrace() {
        return stackTrace;
    }

    public List<RuntimeValue> getRuntimeValueCollection() {
        return runtimeValueCollection;
    }

    public static StackFrameContext forValues(List<RuntimeValue> runtimeValues) {
        return new StackFrameContext(getStacktrace(), runtimeValues);
    }

    public static List<StackFrame> getStacktrace() {
        return StackWalker.getInstance().walk(frames -> frames.dropWhile(StackFrameContext::isOurCode)
                .takeWhile(Predicate.not(StackFrameContext::isOurCode))
                .collect(Collectors.toList()));
    }

    public static String stackFrameToString(StackFrame frame) {
        return frame.getMethodName() + ":" + frame.getLineNumber() + ", " + frame.getClassName();
    }

    private static boolean isOurCode(StackFrame frame) {
        String julianCode = "se.assertteam";
        if (frame.getClassName().startsWith(julianCode)) {
            return true;
        }
        String testCase = "org.junit";
        if (frame.getClassName().startsWith(testCase)) {
            return true;
        }
        return false;
    }
}
