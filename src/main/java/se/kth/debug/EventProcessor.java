package se.kth.debug;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import se.kth.debug.struct.FileAndBreakpoint;
import se.kth.debug.struct.result.BreakPointContext;
import se.kth.debug.struct.result.ReturnData;
import se.kth.debug.struct.result.StackFrameContext;

/** For managing events triggered by JDB. */
public class EventProcessor {
    private static final Logger logger = Logger.getLogger(EventProcessor.class.getName());

    private static final int TIMEOUT = 5000; // milliseconds;
    private final List<BreakPointContext> breakpointContexts = new ArrayList<>();
    private final List<ReturnData> returnValues = new ArrayList<>();
    private final Debugger debugger;

    EventProcessor(String[] providedClasspath, String[] tests, File classesAndBreakpoints) {
        debugger =
                new Debugger(
                        providedClasspath, tests, parseFileAndBreakpoints(classesAndBreakpoints));
    }

    /** Monitor events triggered by JDB. */
    public void startEventProcessor(CollectorOptions context) throws AbsentInformationException {
        VirtualMachine vm = debugger.launchVMAndJunit();
        debugger.addClassPrepareEvent(vm);
        vm.resume();
        try {
            EventSet eventSet;
            while ((eventSet = vm.eventQueue().remove()) != null) {
                for (Event event : eventSet) {
                    if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
                        debugger.getProcess().destroy();
                    }
                    if (event instanceof ClassPrepareEvent) {
                        if (!context.shouldSkipBreakpointValues()) {
                            debugger.setBreakpoints(vm, (ClassPrepareEvent) event);
                        }
                        if (!context.shouldSkipReturnValues()) {
                            debugger.registerMethodExits(vm, (ClassPrepareEvent) event);
                        }
                    }
                    if (event instanceof BreakpointEvent) {
                        List<StackFrameContext> result =
                                debugger.processBreakpoints((BreakpointEvent) event, context);
                        Location location = ((BreakpointEvent) event).location();
                        breakpointContexts.add(
                                new BreakPointContext(
                                        location.sourcePath(), location.lineNumber(), result));
                    }
                    if (event instanceof MethodExitEvent) {
                        ReturnData rd =
                                debugger.processMethodExit((MethodExitEvent) event, context);
                        if (rd != null) {
                            returnValues.add(rd);
                        }
                    }
                }
                vm.resume();
            }
        } catch (VMDisconnectedException | IncompatibleThreadStateException e) {
            logger.warning(e.toString());
        } catch (InterruptedException e) {
            logger.warning(e.toString());
            Thread.currentThread().interrupt();
        }
    }

    private List<FileAndBreakpoint> parseFileAndBreakpoints(File classesAndBreakpoints) {
        if (classesAndBreakpoints == null) {
            return null;
        }
        try (JsonReader jr = new JsonReader(new FileReader(classesAndBreakpoints))) {
            Gson gson = new Gson();
            return gson.fromJson(jr, new TypeToken<List<FileAndBreakpoint>>() {}.getType());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /** Returns the values corresponding to each breakpoint. */
    public List<BreakPointContext> getBreakpointContexts() {
        return breakpointContexts;
    }

    public List<ReturnData> getReturnValues() {
        return returnValues;
    }
}
