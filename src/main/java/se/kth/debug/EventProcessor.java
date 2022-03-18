package se.kth.debug;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import se.kth.debug.struct.FileAndBreakpoint;
import se.kth.debug.struct.result.BreakPointContext;
import se.kth.debug.struct.result.StackFrameContext;

/** For managing events triggered by JDB. */
public class EventProcessor {
    private static final Logger logger = Logger.getLogger(EventProcessor.class.getName());

    private static final int TIMEOUT = 5000; // milliseconds;
    private final List<BreakPointContext> breakpointContexts = new ArrayList<>();
    private final Debugger debugger;

    EventProcessor(
            String[] providedClasspath, String pathToTestDirectory, File classesAndBreakpoints) {
        debugger =
                new Debugger(
                        providedClasspath,
                        pathToTestDirectory,
                        parseFileAndBreakpoints(classesAndBreakpoints));
    }

    /** Monitor events triggered by JDB. */
    public void startEventProcessor() {
        VirtualMachine vm = debugger.launchVMAndJunit();
        debugger.addClassPrepareEvent(vm);
        vm.resume();
        try {
            EventSet eventSet = null;
            while ((eventSet = vm.eventQueue().remove(TIMEOUT)) != null) {
                for (Event event : eventSet) {
                    if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
                        debugger.getProcess().destroy();
                    }
                    if (event instanceof ClassPrepareEvent) {
                        debugger.setBreakpoints(vm, (ClassPrepareEvent) event);
                    }
                    if (event instanceof BreakpointEvent) {
                        List<StackFrameContext> result =
                                debugger.processBreakpoints((BreakpointEvent) event);
                        Location location = ((BreakpointEvent) event).location();
                        breakpointContexts.add(
                                new BreakPointContext(
                                        location.sourcePath(), location.lineNumber(), result));
                    }
                }
                vm.resume();
            }
        } catch (VMDisconnectedException
                | AbsentInformationException
                | IncompatibleThreadStateException e) {
            logger.warning(e.toString());
        } catch (InterruptedException e) {
            logger.warning(e.toString());
            Thread.currentThread().interrupt();
        }
    }

    private List<FileAndBreakpoint> parseFileAndBreakpoints(File classesAndBreakpoints) {
        try (BufferedReader br = new BufferedReader(new FileReader(classesAndBreakpoints))) {
            List<FileAndBreakpoint> parsedFileAndBreakpoints = new ArrayList<>();
            for (String line; (line = br.readLine()) != null; ) {
                String[] fileAndBreakpoints = line.split("=");
                String[] breakpoints = fileAndBreakpoints[1].split(",");
                List<Integer> parsedBreakpoints =
                        Arrays.stream(breakpoints)
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());
                FileAndBreakpoint fNB =
                        new FileAndBreakpoint(fileAndBreakpoints[0], parsedBreakpoints);
                parsedFileAndBreakpoints.add(fNB);
            }
            return parsedFileAndBreakpoints;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /** Returns the values corresponding to each breakpoint. */
    public List<BreakPointContext> getBreakpointContexts() {
        return breakpointContexts;
    }
}
