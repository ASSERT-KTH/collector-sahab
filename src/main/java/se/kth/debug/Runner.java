package se.kth.debug;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Runner {
    private static Logger logger = Logger.getLogger("Runner");

    public static void main(String[] args) {
        Debugger debugger = new Debugger();
        VirtualMachine vm = debugger.launchVMAndJunit();
        debugger.addClassPrepareEvent(vm);
        vm.resume();
        try {
            EventSet eventSet = null;
            while ((eventSet = vm.eventQueue().remove()) != null) {
                for (Event event: eventSet) {
                    if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
                        debugger.process.destroy();
                    }
                    if (event instanceof ClassPrepareEvent) {
                        logger.log(Level.INFO, "Classes are prepared!");
                        debugger.setBreakpoints(vm);
                    }
                    if (event instanceof BreakpointEvent) {
                        logger.log(Level.INFO, "Breakpoint event is reached!");
                        debugger.processBreakpoints(vm, (BreakpointEvent) event);
                    }
                }
                vm.resume();
            }
        } catch (IncompatibleThreadStateException | AbsentInformationException | VMDisconnectedException e) {
            logger.log(Level.WARNING, e.toString());
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, e.toString());
            Thread.currentThread().interrupt();
        }
    }
}
