package io.github.chains_project.collector.util;

import io.github.chains_project.cs.commons.runtime.LineSnapshot;
import io.github.chains_project.cs.commons.runtime.LocalVariable;
import io.github.chains_project.cs.commons.runtime.RuntimeReturnedValue;
import io.github.chains_project.cs.commons.runtime.RuntimeValue;
import io.github.chains_project.cs.commons.runtime.StackFrameContext;
import io.github.chains_project.cs.commons.runtime.output.SahabOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContextCollector {

    private static final ObjectIntrospection INTROSPECTOR = new ObjectIntrospection();
    private static final SahabOutput SAHAB_OUTPUT = new SahabOutput();

    public static SahabOutput getSahabOutput() {
        return SAHAB_OUTPUT;
    }

    public static void setExecutionDepth(int depth) {
        ObjectIntrospection.setExecutionDepth(depth);
    }

    public static List<RuntimeValue> convertLocalVariables(LocalVariable[] variables) {
        try {
            List<RuntimeValue> values = new ArrayList<>();
            for (LocalVariable variable : variables) {
                values.add(INTROSPECTOR.introspectVariable(variable));
            }
            return values;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logLine(
            String className, int lineNumber, Object receiver, LocalVariable[] localVariables, Class<?> receiverClass) {
        try {
            logLineImpl(className, lineNumber, receiver, localVariables, receiverClass);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void logLineImpl(
            String className, int lineNumber, Object receiver, LocalVariable[] localVariables, Class<?> receiverClass)
            throws ReflectiveOperationException {
        List<RuntimeValue> values = convertLocalVariables(localVariables);
        values.addAll(INTROSPECTOR.introspectReceiverFields(receiver, receiverClass));

        StackFrameContext stackFrameContext = StackFrameContext.forValues(values);
        LineSnapshot lineSnapshot = new LineSnapshot(className, lineNumber, List.of(stackFrameContext));

        SAHAB_OUTPUT.getBreakpoint().add(lineSnapshot);
    }

    public static void logReturn(
            Object returnValue,
            String methodName,
            Class<?> returnTypeClass,
            Class<?> receiverClass,
            List<RuntimeValue> parameters) {
        try {
            List<StackWalker.StackFrame> stacktrace = StackFrameContext.getStacktrace();
            RuntimeReturnedValue returned = INTROSPECTOR.introspectReturnValue(
                    methodName,
                    returnValue,
                    parameters,
                    stacktrace.stream()
                            .map(StackFrameContext::stackFrameToString)
                            .collect(Collectors.toList()),
                    StackFrameContext.getLocation(stacktrace),
                    receiverClass,
                    returnTypeClass);
            SAHAB_OUTPUT.getReturns().add(returned);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
