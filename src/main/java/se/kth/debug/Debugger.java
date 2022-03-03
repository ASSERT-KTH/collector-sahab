package se.kth.debug;

import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Debugger {
    private Process process;
    private final Logger logger = Logger.getLogger("Debugger");

    private final String pathToBuiltProject;
    private final String pathToTestDirectory;
    private final List<FileAndBreakpoint> classesAndBreakpoints;

    public Debugger(String pathToBuiltProject, String pathToTestDirectory, List<FileAndBreakpoint> classesAndBreakpoints) {
        this.pathToBuiltProject = pathToBuiltProject;
        this.pathToTestDirectory = pathToTestDirectory;
        this.classesAndBreakpoints = classesAndBreakpoints;
    }

    public VirtualMachine launchVMAndJunit() {
        try {
            String classpath = Utility.getFullClasspath(pathToBuiltProject);
            String testsSeparatedBySpace = Utility.getAllTests(pathToTestDirectory);
            ProcessBuilder processBuilder = new ProcessBuilder("java",
                    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y",
                    "-cp",
                    classpath,
                    MethodTestRunner.class.getCanonicalName(),
                    testsSeparatedBySpace
            );
            logger.log(Level.INFO, "java -cp " + classpath + " " + MethodTestRunner.class.getCanonicalName() + " " + testsSeparatedBySpace);

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
            Runtime.getRuntime().addShutdownHook(new Thread() {
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
        for (FileAndBreakpoint classToBeDebugged: classesAndBreakpoints) {
            ClassPrepareRequest cpr = erm.createClassPrepareRequest();
            cpr.addClassFilter(classToBeDebugged.getFileName());
            cpr.setEnabled(true);
            vm.resume();
        }
    }

    public void setBreakpoints(VirtualMachine vm, ClassPrepareEvent event) throws AbsentInformationException {
        EventRequestManager erm = vm.eventRequestManager();

        List<Integer> breakpoints = classesAndBreakpoints.stream().filter(cb -> cb.getFileName().equals(event.referenceType().name())).findFirst().get().getBreakpoints();

        for (int lineNumber: breakpoints) {
            List<Location> locations = event.referenceType().locationsOfLine(lineNumber);
            BreakpointRequest br = erm.createBreakpointRequest(locations.get(0));
            br.setEnabled(true);
        }
    }

    public List<Object> processBreakpoints(VirtualMachine vm, BreakpointEvent bpe) throws IncompatibleThreadStateException, AbsentInformationException {
        ThreadReference threadReference = bpe.thread();
        StackFrame stackFrame = threadReference.frame(0);

        List<Object> results = new ArrayList<>();

        Map<String, String> localVariablesAsResults = new HashMap<>();
        List<LocalVariable> localVariables = stackFrame.visibleVariables();
        for (LocalVariable localVariable: localVariables) {
            Value value = stackFrame.getValue(localVariable);
            localVariablesAsResults.put(localVariable.name(), value.toString());
        }
        Map<String, Map<String, String>> m1 = new HashMap<>();
        m1.put("Local Variables", localVariablesAsResults);
        results.add(m1);

        Map<String, String> fieldsAsResults = new HashMap<>();
        List<Field> fields = stackFrame.location().declaringType().visibleFields();
        for (Field field : fields) {
            Value value;
            if (field.isStatic()) {
                value = stackFrame.location().declaringType().getValue(field);
            } else {
                value = stackFrame.thisObject().getValue(field);
            }
            fieldsAsResults.put(field.name(), value.toString());
        }
        Map<String, Map<String, String>> m2 = new HashMap<>();
        m2.put("Fields", fieldsAsResults);
        results.add(m2);

        return results;
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
