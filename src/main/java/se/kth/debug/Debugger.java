package se.kth.debug;

import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Debugger {
    private Process process;
    private final Logger logger = Logger.getLogger("Debugger");

    public VirtualMachine launchVMAndJunit() {
        String requiredClasspath = "";
        String testList = "se.kth.debug.AppTest";
        try {
            String classpath = JavaUtils.getFullClasspath(requiredClasspath);
            ProcessBuilder processBuilder = new ProcessBuilder("java",
                    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y",
                    "-cp",
                    classpath,
                    MethodTestRunner.class.getCanonicalName(),
                    testList
            );
            logger.log(Level.INFO, "java -cp " + classpath + " " + MethodTestRunner.class.getCanonicalName() + " " + testList);

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
        ClassPrepareRequest cpr = erm.createClassPrepareRequest();
        cpr.addClassFilter("se.kth.debug.App");
        cpr.setEnabled(true);
        vm.resume();
    }

    public void setBreakpoints(VirtualMachine vm) throws AbsentInformationException {
        EventRequestManager erm = vm.eventRequestManager();
        List<ReferenceType> referenceTypes = vm.classesByName("se.kth.debug.App");
        List<Location> locations = referenceTypes.get(0).locationsOfLine(5);
        BreakpointRequest br = erm.createBreakpointRequest(locations.get(0));
        br.setEnabled(true);
    }

    public void processBreakpoints(VirtualMachine vm, BreakpointEvent bpe) throws IncompatibleThreadStateException, AbsentInformationException {
        ThreadReference threadReference = bpe.thread();
        StackFrame stackFrame = threadReference.frame(0);

        List<LocalVariable> localVariables = stackFrame.visibleVariables();
        for (LocalVariable localVariable: localVariables) {
            Value value = stackFrame.getValue(localVariable);
            logger.log(Level.INFO, localVariable.name() + "::" + value.toString());
        }
    }

    public void shutdown(VirtualMachine vm) {
        try {
            process.destroy();
            // process.waitFor();
            vm.exit(0);
        } catch (Exception e) {
            // ignore
        }
    }

    public Process getProcess() {
        return process;
    }
}
