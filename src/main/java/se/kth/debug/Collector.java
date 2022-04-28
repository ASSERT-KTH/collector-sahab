package se.kth.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.jdi.AbsentInformationException;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import picocli.CommandLine;
import se.kth.debug.struct.result.*;

@CommandLine.Command(name = "collector", mixinStandardHelpOptions = true)
public class Collector implements Callable<Integer> {
    private static final Logger logger = Logger.getLogger("Runner");

    @CommandLine.Option(
            names = "-p",
            arity = "0..*",
            description = "Classpath required to run JUnit",
            split = " ")
    private String[] providedClasspath;

    @CommandLine.Option(
            names = "-t",
            arity = "1..*",
            description = "List of test methods",
            split = " ")
    private String[] tests;

    @CommandLine.Option(names = "-o", description = "Path to output file (JSON)")
    private static String collectedOutput;

    @CommandLine.Option(names = "-i", description = "File containing class names and breakpoints")
    private static File classesAndBreakpoints = null;

    @CommandLine.Option(names = "-m", description = "File containing method name")
    private static File methodForExitEvent = null;

    @CommandLine.Option(
            names = "--stack-trace-depth",
            description =
                    "The depth of stack trace the data needs to be collected from (default: ${DEFAULT-VALUE}).")
    private int stackTraceDepth = 1;

    @CommandLine.Option(
            names = "--number-of-array-elements",
            description =
                    "Number of elements that need to be printed inside an array (default: ${DEFAULT-VALUE}).")
    private int numberOfArrayElements = 10;

    @CommandLine.Option(
            names = "--execution-depth",
            description = "The depth of each element inside an array (default: ${DEFAULT-VALUE}).")
    private int executionDepth = 0;

    @CommandLine.Option(
            names = "--skip-printing-field",
            description = "Whether to collect field data or not")
    private boolean skipPrintingField = false;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Collector()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws IOException, AbsentInformationException {
        CollectorOptions context = getCollectorOptions();
        EventProcessor eventProcessor =
                invoke(
                        providedClasspath,
                        tests,
                        classesAndBreakpoints,
                        methodForExitEvent,
                        context);
        write(eventProcessor);
        return 0;
    }

    public static EventProcessor invoke(
            String[] providedClasspath,
            String[] tests,
            File classesAndBreakpoints,
            File methodForExitEvent,
            CollectorOptions context)
            throws AbsentInformationException {
        EventProcessor eventProcessor =
                new EventProcessor(
                        providedClasspath, tests, classesAndBreakpoints, methodForExitEvent);
        eventProcessor.startEventProcessor(context);

        return eventProcessor;
    }

    private CollectorOptions getCollectorOptions() {
        CollectorOptions context = new CollectorOptions();
        context.setStackTraceDepth(stackTraceDepth);
        context.setNumberOfArrayElements(numberOfArrayElements);
        context.setExecutionDepth(executionDepth);
        context.setSkipPrintingField(skipPrintingField);

        return context;
    }

    public static void write(EventProcessor eventProcessor) throws IOException {
        final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        JsonObject output = new JsonObject();

        if (classesAndBreakpoints == null) {
            logger.info(
                    "Breakpoint data was not asked for. Please provide class names and line numbers if you desire otherwise.");
        } else if (!eventProcessor.getBreakpointContexts().isEmpty()) {
            output.add("breakpoint", gson.toJsonTree(eventProcessor.getBreakpointContexts()));
            logger.info("Breakpoints serialised!");
        } else {
            logger.info("Output file was not generated as breakpoints were not visited.");
        }

        if (methodForExitEvent == null) {
            logger.info(
                    "Return data was not asked for. Please provide method names if you desire otherwise.");
        } else if (!eventProcessor.getReturnValues().isEmpty()) {
            output.add("return", gson.toJsonTree(eventProcessor.getReturnValues()));
            logger.info("Return values serialised!");
        } else {
            logger.info("No method exits were encountered.");
        }

        File file = new File(collectedOutput);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (FileWriter writer = new FileWriter(collectedOutput)) {
            writer.write(gson.toJson(output));
            logger.info("File output to: " + file.getAbsolutePath());
        }
    }
}
