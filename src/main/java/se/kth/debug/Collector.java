package se.kth.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    @CommandLine.Option(names = "-b", description = "Path to output file (JSON)")
    private static String breakpointJson;

    @CommandLine.Option(
            names = "-r",
            description = "File containing all return values of methods in the provided class.")
    private static String returnValueJson;

    @CommandLine.Option(
            names = "-i",
            description = "File containing class names and breakpoints",
            defaultValue = "input.txt")
    private File classesAndBreakpoints;

    @CommandLine.Option(
            names = "--object-depth",
            description = "Nesting level of object required (default: ${DEFAULT-VALUE}).")
    private int objectDepth = 0;

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
            names = "--array-depth",
            description = "The depth of each element inside an array (default: ${DEFAULT-VALUE}).")
    private int arrayDepth = 0;

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
                invoke(providedClasspath, tests, classesAndBreakpoints, context);
        write(eventProcessor);
        return 0;
    }

    public static EventProcessor invoke(
            String[] providedClasspath,
            String[] tests,
            File classesAndBreakpoints,
            CollectorOptions context)
            throws AbsentInformationException {
        EventProcessor eventProcessor =
                new EventProcessor(providedClasspath, tests, classesAndBreakpoints);
        eventProcessor.startEventProcessor(context);

        return eventProcessor;
    }

    private CollectorOptions getCollectorOptions() {
        CollectorOptions context = new CollectorOptions();
        context.setObjectDepth(objectDepth);
        context.setStackTraceDepth(stackTraceDepth);
        context.setNumberOfArrayElements(numberOfArrayElements);
        context.setArrayDepth(arrayDepth);
        context.setSkipPrintingField(skipPrintingField);

        return context;
    }

    public static void write(EventProcessor eventProcessor) throws IOException {
        if (!eventProcessor.getBreakpointContexts().isEmpty()) {
            writeBreakpointsToFile(eventProcessor.getBreakpointContexts());
            logger.info("Output file generated!");
        } else {
            logger.info("Output file was not generated as breakpoints were not visited.");
        }
        if (!eventProcessor.getReturnValues().isEmpty()) {
            writeReturnValuesToFile(eventProcessor.getReturnValues());
            logger.info("Return values are output to the file!");
        } else {
            logger.info("No method exits were encountered.");
        }
    }

    private static void writeBreakpointsToFile(List<BreakPointContext> breakPointContext)
            throws IOException {
        final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        File file = new File(breakpointJson);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (FileWriter writer = new FileWriter(breakpointJson)) {
            writer.write(gson.toJson(breakPointContext));
        }
    }

    private static void writeReturnValuesToFile(List<ReturnData> returnValues) throws IOException {
        final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        File file = new File(returnValueJson);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (FileWriter writer = new FileWriter(returnValueJson)) {
            writer.write(gson.toJson(returnValues));
        }
    }
}
