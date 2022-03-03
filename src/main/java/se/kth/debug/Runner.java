package se.kth.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.reflect.Modifier.TRANSIENT;

public class Runner {
    private static final Logger logger = Logger.getLogger("Runner");

    private static final List<Result> results = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        Debugger debugger = new Debugger();
        VirtualMachine vm = debugger.launchVMAndJunit();
        debugger.addClassPrepareEvent(vm);
        vm.resume();
        try {
            EventSet eventSet = null;
            while ((eventSet = vm.eventQueue().remove()) != null) {
                for (Event event: eventSet) {
                    if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
                        debugger.getProcess().destroy();
                    }
                    if (event instanceof ClassPrepareEvent) {
                        logger.log(Level.INFO, "Classes are prepared!");
                        debugger.setBreakpoints(vm);
                    }
                    if (event instanceof BreakpointEvent) {
                        logger.log(Level.INFO, "Breakpoint event is reached!");
                        List<Object> result = debugger.processBreakpoints(vm, (BreakpointEvent) event);
                        Location location = ((BreakpointEvent) event).location();
                        results.add(new Result(location.sourcePath(), location.lineNumber(), result));
                    }
                }
                vm.resume();
            }
        } catch (VMDisconnectedException | AbsentInformationException | IncompatibleThreadStateException e) {
            logger.log(Level.WARNING, e.toString());
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, e.toString());
            Thread.currentThread().interrupt();
        } finally {
            final Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .excludeFieldsWithoutExposeAnnotation()
                    .excludeFieldsWithModifiers(TRANSIENT)
                    .create();
            FileWriter file = new FileWriter("output.json");
            file.write(gson.toJson(results));
            file.flush();
            file.close();
        }
    }
}
