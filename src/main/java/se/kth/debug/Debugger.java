package se.kth.debug;

import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodExitRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import se.kth.debug.struct.FileAndBreakpoint;
import se.kth.debug.struct.result.*;

public class Debugger {
    private Process process;
    private final Logger logger = Logger.getLogger("Debugger");

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

    public List<StackFrameContext> processBreakpoints(
            BreakpointEvent bpe, int objectDepth, int stackTraceDepth, int numberOfArrayElements)
            throws IncompatibleThreadStateException, AbsentInformationException {
        ThreadReference threadReference = bpe.thread();

        int framesToBeProcessed = stackTraceDepth;
        if (stackTraceDepth > threadReference.frameCount()) {
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
                List<LocalVariableData> localVariables =
                        collectLocalVariable(stackFrame, objectDepth, numberOfArrayElements);
                stackFrameContext.addRuntimeValueCollection(localVariables);

                List<FieldData> fields =
                        collectFields(stackFrame, objectDepth, numberOfArrayElements);
                stackFrameContext.addRuntimeValueCollection(fields);

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

    public ReturnData processMethodExit(
            MethodExitEvent mee, int objectDepth, int numberOfArrayElements)
            throws IncompatibleThreadStateException, AbsentInformationException {
        String methodName = mee.method().name();
        String returnType = mee.method().returnTypeName();
        String returnValue = getStringRepresentation(mee.returnValue(), numberOfArrayElements);
        String location = mee.location().toString();
        List<LocalVariable> arguments = mee.method().arguments();

        ReturnData returnData =
                new ReturnData(
                        methodName,
                        returnType,
                        returnValue,
                        location,
                        // the method will be in the 0th stack frame when the method exit event is
                        // triggered
                        collectArguments(
                                mee.thread().frame(0),
                                arguments,
                                objectDepth,
                                numberOfArrayElements),
                        computeStackTrace(mee.thread()));
        if (isAnObjectReference(mee.returnValue())) {
            returnData.setNestedTypes(
                    getNestedFields(
                            (ObjectReference) mee.returnValue(),
                            objectDepth,
                            numberOfArrayElements));
        }
        return returnData;
    }

    private List<LocalVariableData> collectArguments(
            StackFrame stackFrame,
            List<LocalVariable> arguments,
            int objectDepth,
            int numberOfArrayElements) {
        return parseVariable(stackFrame, arguments, objectDepth, numberOfArrayElements);
    }

    private List<LocalVariableData> collectLocalVariable(
            StackFrame stackFrame, int objectDepth, int numberOfArrayElements)
            throws AbsentInformationException {
        return parseVariable(
                stackFrame, stackFrame.visibleVariables(), objectDepth, numberOfArrayElements);
    }

    private List<LocalVariableData> parseVariable(
            StackFrame stackFrame,
            List<LocalVariable> variables,
            int objectDepth,
            int numberOfArrayElements) {
        List<LocalVariableData> result = new ArrayList<>();
        for (LocalVariable variable : variables) {
            Value value = stackFrame.getValue(variable);
            LocalVariableData localVariableData =
                    new LocalVariableData(
                            variable.name(),
                            variable.typeName(),
                            getStringRepresentation(value, numberOfArrayElements));
            result.add(localVariableData);
            if (isAnObjectReference(value)) {
                localVariableData.setNestedTypes(
                        getNestedFields(
                                (ObjectReference) value, objectDepth, numberOfArrayElements));
            }
        }
        return result;
    }

    private List<FieldData> collectFields(
            StackFrame stackFrame, int objectDepth, int numberOfArrayElements) {
        List<FieldData> result = new ArrayList<>();

        List<Field> visibleFields = stackFrame.location().declaringType().visibleFields();
        for (Field field : visibleFields) {
            Value value;
            if (field.isStatic()) {
                value = stackFrame.location().declaringType().getValue(field);
            } else if (stackFrame.location().declaringType().isStatic()) {
                logger.warning("Cannot get fields of static inner classes");
                value = null;
            } else {
                value = stackFrame.thisObject().getValue(field);
            }
            FieldData fieldData =
                    new FieldData(
                            field.name(),
                            field.typeName(),
                            getStringRepresentation(value, numberOfArrayElements));
            result.add(fieldData);
            if (isAnObjectReference(value)) {
                fieldData.setNestedTypes(
                        getNestedFields(
                                (ObjectReference) value, objectDepth, numberOfArrayElements));
            }
        }
        return result;
    }

    private List<FieldData> getNestedFields(
            ObjectReference object, int objectDepth, int numberOfArrayElements) {
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
                            field.typeName(),
                            getStringRepresentation(value, numberOfArrayElements));
            result.add(fieldData);
            if (isAnObjectReference(value)) {
                fieldData.setNestedTypes(
                        getNestedFields(
                                (ObjectReference) value, objectDepth - 1, numberOfArrayElements));
            }
        }
        return result;
    }

    private static String getStringRepresentation(Value value, int numberOfArrayElements) {
        if (value instanceof ArrayReference) {
            List<String> itemsToString =
                    ((ArrayReference) value)
                            .getValues().stream()
                                    .limit(numberOfArrayElements)
                                    .map(
                                            v ->
                                                    Debugger.getStringRepresentation(
                                                            v, numberOfArrayElements))
                                    .collect(Collectors.toList());
            return String.valueOf(itemsToString);
        }
        if (isAnObjectReference(value)) {
            // we print type for object references
            return String.valueOf(((ObjectReference) value).referenceType().name());
        }
        return String.valueOf(value);
    }

    private static boolean isAnObjectReference(Value value) {
        if (value instanceof ObjectReference) {
            List<Class<?>> excludedClasses =
                    List.of(
                            String.class,
                            Integer.class,
                            Long.class,
                            Double.class,
                            Float.class,
                            Boolean.class,
                            Character.class,
                            Byte.class,
                            Void.class,
                            Short.class);
            try {
                return !excludedClasses.contains(Class.forName(value.type().name()));
            } catch (ClassNotFoundException exception) {
                return true;
            }
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
