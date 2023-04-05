package se.assertteam.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import se.assertteam.runtime.LineSnapshot;
import se.assertteam.runtime.LocalVariable;
import se.assertteam.runtime.RuntimeReturnedValue;
import se.assertteam.runtime.RuntimeValue;
import se.assertteam.runtime.StackFrameContext;
import se.assertteam.runtime.output.SahabOutput;

public class ContextCollector {

    private static final ObjectIntrospection INTROSPECTOR = new ObjectIntrospection();
    private static final SahabOutput SAHAB_OUTPUT = new SahabOutput();

    public static SahabOutput getSahabOutput() {
        return SAHAB_OUTPUT;
    }

    public static void setExecutionDepth(int depth) {
        INTROSPECTOR.setExecutionDepth(depth);
    }

    public static void logLine(
            String className, int lineNumber, Object receiver, LocalVariable[] localVariables, String receiverClass) {
        try {
            logLineImpl(className, lineNumber, receiver, localVariables, Class.forName(receiverClass));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void logLineImpl(
            String className, int lineNumber, Object receiver, LocalVariable[] localVariables, Class<?> receiverClass)
            throws ReflectiveOperationException {
        List<RuntimeValue> values = new ArrayList<>();
        for (LocalVariable variable : localVariables) {
            values.add(INTROSPECTOR.introspectVariable(variable));
        }

        values.addAll(INTROSPECTOR.introspectReceiverFields(receiver, receiverClass));

        StackFrameContext stackFrameContext = StackFrameContext.forValues(values);
        LineSnapshot lineSnapshot = new LineSnapshot(className, lineNumber, List.of(stackFrameContext));

        SAHAB_OUTPUT.getBreakpoint().add(lineSnapshot);
    }

    public static void logReturn(
            Object returnValue,
            String methodName,
            String returnTypeName,
            String className,
            LocalVariable[] parameters) {
        try {
            List<StackWalker.StackFrame> stacktrace = StackFrameContext.getStacktrace();
            List<RuntimeValue> arguments = new ArrayList<>();
            for (LocalVariable parameter : parameters) {
                arguments.add(INTROSPECTOR.introspectVariable(parameter));
            }
            RuntimeReturnedValue returned = INTROSPECTOR.introspectReturnValue(
                    methodName,
                    returnValue,
                    arguments,
                    stacktrace.stream()
                            .map(StackFrameContext::stackFrameToString)
                            .collect(Collectors.toList()),
                    StackFrameContext.getLocation(stacktrace),
                    Class.forName(className),
                    Classes.getClassFromString(returnTypeName));
            SAHAB_OUTPUT.getReturns().add(returned);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
