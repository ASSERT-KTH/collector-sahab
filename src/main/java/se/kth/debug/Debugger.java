package se.kth.debug;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import se.kth.debug.struct.FileAndBreakpoint;
import se.kth.debug.struct.result.*;

public class Debugger {
    private Process process;
    private static final Logger logger = Logger.getLogger("Debugger");

    private final String[] pathToBuiltProject;
    private final String[] tests;
    private final List<FileAndBreakpoint> classesAndBreakpoints;

    public Debugger(
            String[] pathToBuiltProject,
            String[] tests,
            List<FileAndBreakpoint> classesAndBreakpoints) {
        this.pathToBuiltProject = pathToBuiltProject;
        this.tests = tests;
        this.classesAndBreakpoints = classesAndBreakpoints;
    }

    public VirtualMachine launchVMAndJunit() {
        try {
            String classpath = Utility.getClasspathForRunningJUnit(pathToBuiltProject);
            String testsSeparatedBySpace = Utility.parseTests(tests);
            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            "java",
                            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y",
                            "-cp",
                            classpath,
                            MethodTestRunner.class.getCanonicalName(),
                            testsSeparatedBySpace);
            logger.log(
                    Level.INFO,
                    "java -cp "
                            + classpath
                            + " "
                            + MethodTestRunner.class.getCanonicalName()
                            + " "
                            + testsSeparatedBySpace);

            process = processBuilder.start();

            InputStreamReader isr = new InputStreamReader(process.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String lineRead = br.readLine();
            Pattern pattern = Pattern.compile("([0-9]{4,})");
            Matcher matcher = pattern.matcher(lineRead);
            matcher.find();
            int port = Integer.parseInt(matcher.group());

            final VirtualMachine vm = new VMAcquirer().connect(port);
            logger.log(Level.INFO, "Connected to port: " + port);
            // kill process when the program exit
            Runtime.getRuntime()
                    .addShutdownHook(
                            new Thread() {
                                public void run() {
                                    shutdown(vm);
                                }
                            });
            return vm;
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Wrong URL: " + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addClassPrepareEvent(VirtualMachine vm) {
        EventRequestManager erm = vm.eventRequestManager();
        for (FileAndBreakpoint classToBeDebugged : classesAndBreakpoints) {
            ClassPrepareRequest cpr = erm.createClassPrepareRequest();
            cpr.addClassFilter(classToBeDebugged.getFileName());
            cpr.setEnabled(true);
            logger.log(Level.INFO, classToBeDebugged.getFileName() + " added!");
            vm.resume();
        }
    }

    public void setBreakpoints(VirtualMachine vm, ClassPrepareEvent event)
            throws AbsentInformationException {
        EventRequestManager erm = vm.eventRequestManager();

        List<Integer> breakpoints =
                classesAndBreakpoints.stream()
                        .filter(cb -> cb.getFileName().equals(event.referenceType().name()))
                        .findFirst()
                        .get()
                        .getBreakpoints();

        for (int lineNumber : breakpoints) {
            List<Location> locations = event.referenceType().locationsOfLine(lineNumber);
            BreakpointRequest br = erm.createBreakpointRequest(locations.get(0));
            br.setEnabled(true);
        }
    }

    public void registerMethodExits(VirtualMachine vm, ClassPrepareEvent event) {
        EventRequestManager erm = vm.eventRequestManager();
        MethodExitRequest mer = erm.createMethodExitRequest();
        mer.addClassFilter(event.referenceType());
        mer.setEnabled(true);
    }

    public List<StackFrameContext> processBreakpoints(BreakpointEvent bpe, CollectorOptions context)
            throws IncompatibleThreadStateException, AbsentInformationException {
        ThreadReference threadReference = bpe.thread();

        int framesToBeProcessed = context.getStackTraceDepth();
        if (context.getStackTraceDepth() > threadReference.frameCount()) {
            framesToBeProcessed = threadReference.frameCount();
            logger.warning(
                    String.format(
                            "Stack trace depth cannot be larger than actual. Processing %d frames instead.",
                            framesToBeProcessed));
        }

        List<StackFrameContext> stackFrameContexts = new ArrayList<>();
        for (int i = 0; i < framesToBeProcessed; ++i) {
            StackFrame stackFrame = threadReference.frame(i);
            StackFrameContext stackFrameContext =
                    new StackFrameContext(
                            i + 1,
                            stackFrame.location().toString(),
                            computeStackTrace(threadReference));
            try {
                List<LocalVariableData> localVariables = collectLocalVariable(stackFrame, context);
                stackFrameContext.addRuntimeValueCollection(localVariables);

                if (!context.shouldSkipPrintingField()) {
                    List<FieldData> fields = collectFields(stackFrame, context);
                    stackFrameContext.addRuntimeValueCollection(fields);
                }

                stackFrameContexts.add(stackFrameContext);
            } catch (AbsentInformationException e) {
                if (i == 0) {
                    throw new AbsentInformationException(
                            "The files corresponding to provided breakpoints are not compiled with debugging information.");
                }
                logger.warning(
                        "Information does not exist for " + stackFrame + " and frames later on");
                return stackFrameContexts;
            }
        }
        return stackFrameContexts;
    }

    private List<String> computeStackTrace(ThreadReference threadReference)
            throws IncompatibleThreadStateException {
        List<String> result = new ArrayList<>();
        List<String> excludedPackages =
                List.of("java.lang", "java.util", "org.junit", "junit", "jdk");
        for (StackFrame stackFrame : threadReference.frames()) {
            Location location = stackFrame.location();
            String declaringTypeName = location.declaringType().name();
            if (excludedPackages.stream().filter(declaringTypeName::contains).findAny().isEmpty()) {
                String output =
                        String.format(
                                "%s:%d, %s",
                                location.method().name(), location.lineNumber(), declaringTypeName);
                result.add(output);
            }
        }
        return result;
    }

    public ReturnData processMethodExit(MethodExitEvent mee, CollectorOptions context)
            throws IncompatibleThreadStateException, AbsentInformationException {
        String methodName = mee.method().name();
        String location = mee.location().toString();
        List<LocalVariable> arguments = mee.method().arguments();

        ReturnData returnData =
                new ReturnData(
                        methodName,
                        constructValue(
                                mee.returnValue(),
                                mee.method().returnTypeName(),
                                context.getObjectDepth(),
                                context.getArrayDepth(),
                                context),
                        location,
                        // the method will be in the 0th stack frame when the method exit event is
                        // triggered
                        collectArguments(mee.thread().frame(0), arguments, context),
                        computeStackTrace(mee.thread()));
        if (isAnObjectReference(mee.returnValue())) {
            returnData.setFields(
                    getNestedFields(
                            (ObjectReference) mee.returnValue(),
                            context.getObjectDepth(),
                            context.getArrayDepth(),
                            context));
        }
        return returnData;
    }

    private List<LocalVariableData> collectArguments(
            StackFrame stackFrame, List<LocalVariable> arguments, CollectorOptions context) {
        return parseVariable(stackFrame, arguments, context);
    }

    private List<LocalVariableData> collectLocalVariable(
            StackFrame stackFrame, CollectorOptions context) throws AbsentInformationException {
        return parseVariable(stackFrame, stackFrame.visibleVariables(), context);
    }

    private static Object getReadableValue(Value value) {
        if (value == null) {
            return null;
        }
        if (value instanceof IntegerValue) {
            return ((IntegerValue) value).value();
        } else if (value instanceof BooleanValue) {
            return ((BooleanValue) value).value();
        } else if (value instanceof FloatValue) {
            return ((FloatValue) value).value();
        } else if (value instanceof StringReference) {
            return ((StringReference) value).value();
        } else if (value instanceof ShortValue) {
            return ((ShortValue) value).value();
        } else if (value instanceof LongValue) {
            return ((LongValue) value).value();
        } else if (value instanceof CharValue) {
            return ((CharValue) value).value();
        } else if (isPrimitiveWrapper(value)) {
            Field field = ((ObjectReference) value).referenceType().fieldByName("value");
            Value nestedValue = ((ObjectReference) value).getValue(field);
            return getReadableValue(nestedValue);
        } else {
            return String.valueOf(((ObjectReference) value).referenceType().name());
        }
    }

    private List<LocalVariableData> parseVariable(
            StackFrame stackFrame, List<LocalVariable> variables, CollectorOptions context) {
        List<LocalVariableData> result = new ArrayList<>();
        for (LocalVariable variable : variables) {
            Value value = stackFrame.getValue(variable);
            LocalVariableData localVariableData =
                    new LocalVariableData(
                            variable.name(),
                            constructValue(
                                    value,
                                    variable.typeName(),
                                    context.getObjectDepth(),
                                    context.getArrayDepth(),
                                    context));
            result.add(localVariableData);
            if (isAnObjectReference(value)) {
                localVariableData.setFields(
                        getNestedFields(
                                (ObjectReference) value,
                                context.getObjectDepth(),
                                context.getArrayDepth(),
                                context));
            }
        }
        return result;
    }

    private List<FieldData> collectFields(StackFrame stackFrame, CollectorOptions context) {
        List<FieldData> result = new ArrayList<>();

        List<Field> visibleFields = stackFrame.location().declaringType().visibleFields();
        for (Field field : visibleFields) {
            Value value;
            if (field.isStatic()) {
                value = stackFrame.location().declaringType().getValue(field);
            } else if (stackFrame.location().method().isStatic()) {
                // Since we are inside a static method, we don't have access to the non-static
                // field.
                // Hence, we are skipping its collection.
                continue;
            } else {
                value = stackFrame.thisObject().getValue(field);
            }
            FieldData fieldData =
                    new FieldData(
                            field.name(),
                            constructValue(
                                    value,
                                    field.typeName(),
                                    context.getObjectDepth(),
                                    context.getArrayDepth(),
                                    context));
            if (isAnObjectReference(value)) {
                fieldData.setFields(
                        getNestedFields(
                                (ObjectReference) value,
                                context.getObjectDepth(),
                                context.getArrayDepth(),
                                context));
            }
            result.add(fieldData);
        }
        return result;
    }

    private static ValueWrapper constructValue(
            Value value,
            String typeName,
            int objectDepth,
            int arrayDepth,
            CollectorOptions context) {
        if (value instanceof ArrayReference) {
            List<Value> neededValues =
                    ((ArrayReference) value)
                            .getValues().stream()
                                    .filter(Objects::nonNull)
                                    .limit(context.getNumberOfArrayElements())
                                    .collect(Collectors.toList());
            ValueWrapper topLevelArrayWrtToRecursion =
                    new ValueWrapper(
                            typeName,
                            neededValues.stream()
                                    .map(Debugger::getReadableValue)
                                    .collect(Collectors.toList()));
            if (arrayDepth > 0) {
                List<ValueWrapper> nestedObjects = new ArrayList<>();
                for (Value nestedValue : neededValues) {
                    if (nestedValue instanceof ArrayReference) {
                        ValueWrapper vw =
                                constructValue(
                                        nestedValue,
                                        nestedValue.type().name(),
                                        objectDepth,
                                        arrayDepth - 1,
                                        context);
                        nestedObjects.add(vw);
                    } else if (isAnObjectReference(nestedValue)) {
                        ValueWrapper vw =
                                new ValueWrapper(
                                        nestedValue.type().name(), getReadableValue(value));
                        vw.setNestedObjects(
                                getNestedFields(
                                        (ObjectReference) nestedValue,
                                        objectDepth - 1,
                                        arrayDepth,
                                        context));
                        nestedObjects.add(vw);
                    } else {
                        ValueWrapper vw =
                                new ValueWrapper(
                                        nestedValue.type().name(), getReadableValue(nestedValue));
                        nestedObjects.add(vw);
                    }
                }
                topLevelArrayWrtToRecursion.setNestedObjects(nestedObjects);
            }
            return topLevelArrayWrtToRecursion;
        }
        return new ValueWrapper(typeName, getReadableValue(value));
    }

    private static List<FieldData> getNestedFields(
            ObjectReference object, int objectDepth, int arrayDepth, CollectorOptions context) {
        if (objectDepth == 0) {
            return null;
        }
        List<FieldData> result = new ArrayList<>();
        List<Field> fields = object.referenceType().visibleFields();
        for (Field field : fields) {
            Value value = object.getValue(field);

            FieldData fieldData =
                    new FieldData(
                            field.name(),
                            constructValue(
                                    value, field.typeName(), objectDepth, arrayDepth - 1, context));
            result.add(fieldData);
            if (isAnObjectReference(value)) {
                fieldData.setFields(
                        getNestedFields(
                                (ObjectReference) value, objectDepth - 1, arrayDepth, context));
            }
        }
        return result;
    }

    private static boolean isPrimitiveWrapper(Value value) {
        // Void.class is not here because it does not matter as this function is supposed to decide
        // how the object
        // will be printed. Since void does not have any value, we do not need to determine how its
        // printing will be handled.
        List<Class<?>> included =
                List.of(
                        String.class,
                        Integer.class,
                        Long.class,
                        Double.class,
                        Float.class,
                        Boolean.class,
                        Character.class,
                        Byte.class,
                        Short.class);
        try {
            return included.contains(Class.forName(value.type().name()));
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static boolean isAnObjectReference(Value value) {
        if (value instanceof ObjectReference) {
            return !isPrimitiveWrapper(value);
        }
        return false;
    }

    public void shutdown(VirtualMachine vm) {
        try {
            process.destroy();
            vm.exit(0);
        } catch (Exception e) {
            // ignore
        }
    }

    public Process getProcess() {
        return process;
    }
}
