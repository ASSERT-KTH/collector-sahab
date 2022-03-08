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
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import picocli.CommandLine;
import se.kth.debug.struct.FileAndBreakpoint;
import se.kth.debug.struct.result.*;

@CommandLine.Command(name = "collector", mixinStandardHelpOptions = true)
public class Collector implements Callable<Integer> {
    private static final Logger logger = Logger.getLogger("Runner");

    private static final List<BreakPointContext> breakpointContexts = new ArrayList<>();

    @CommandLine.Option(names = "-p", description = "Path to the the compiled project")
    private String pathToBuiltProject;

    @CommandLine.Option(names = "-t", description = "Path to test directory")
    private String pathToTestDirectory;

    @CommandLine.Option(names = "-o", description = "Path to output file (JSON)")
    private String pathToOutputJson = "output.json";

    @CommandLine.Option(
            names = "-i",
            description = "File containing class names and breakpoints",
            defaultValue = "input.txt")
    private File classesAndBreakpoints;

    private List<FileAndBreakpoint> parseFileAndBreakpoints() {
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Integer call() throws Exception {
        Debugger debugger =
                new Debugger(pathToBuiltProject, pathToTestDirectory, parseFileAndBreakpoints());
        VirtualMachine vm = debugger.launchVMAndJunit();
        debugger.addClassPrepareEvent(vm);
        vm.resume();
        try {
            EventSet eventSet = null;
            while ((eventSet = vm.eventQueue().remove()) != null) {
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
            logger.log(Level.WARNING, e.toString());
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, e.toString());
            Thread.currentThread().interrupt();
        } finally {
            final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
            FileWriter file = new FileWriter(pathToOutputJson);
            file.write(gson.toJson(breakpointContexts));
            file.flush();
            file.close();
        }
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Collector()).execute(args);
        System.exit(exitCode);
    }
}
