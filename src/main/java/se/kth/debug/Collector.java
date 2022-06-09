package se.kth.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.sun.jdi.AbsentInformationException;
import java.io.*;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import picocli.CommandLine;
import se.kth.debug.struct.result.BreakPointContext;
import se.kth.debug.struct.result.ReturnData;

@CommandLine.Command(name = "collector", mixinStandardHelpOptions = true)
public class Collector implements Callable<Integer> {
    private static final Logger logger = Logger.getLogger("Runner");

    @CommandLine.Option(
            names = "-p",
            arity = "0..*",
            description = "Classpath required to run JUnit",
            split = " ",
            required = true)
    private String[] providedClasspath;

    @CommandLine.Option(
            names = "-t",
            arity = "1..*",
            description = "List of test methods",
            split = " ",
            required = true)
    private String[] tests;

    @CommandLine.Option(names = "-o", description = "Path to output file (JSON)", required = true)
    private String collectedOutput;

    @CommandLine.Option(
            names = "-i",
            description = "File containing class names and breakpoints",
            required = true)
    private File classesAndBreakpoints = null;

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
            description = "Whether to collect field data or not (default: ${DEFAULT-VALUE}).")
    private boolean skipPrintingField = false;

    @CommandLine.Option(
            names = "--skip-breakpoint-values",
            description = "Whether to collect breakpoint values (default: ${DEFAULT-VALUE}).")
    private boolean skipBreakpointValues = false;

    @CommandLine.Option(
            names = "--skip-return-values",
            description = "Whether to collect return values (default: ${DEFAULT-VALUE}).")
    private boolean skipReturnValues = false;

    public static void main(String[] args) {
        new CommandLine(new Collector()).execute(args);
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
        context.setStackTraceDepth(stackTraceDepth);
        context.setNumberOfArrayElements(numberOfArrayElements);
        context.setExecutionDepth(executionDepth);
        context.setSkipPrintingField(skipPrintingField);
        context.setSkipBreakpointValues(skipBreakpointValues);
        context.setSkipReturnValues(skipReturnValues);

        return context;
    }

    public void write(EventProcessor eventProcessor) throws IOException {
        final Gson gson =
                new GsonBuilder()
                        .setPrettyPrinting()
                        .serializeNulls()
                        .serializeSpecialFloatingPointValues()
                        .create();

        File file = new File(collectedOutput);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        JsonWriter writer = new JsonWriter(new FileWriter(file));

        writer.setIndent("  ");
        writer.beginObject();

        if (skipBreakpointValues) {
            logger.info(
                    "Breakpoint data was not asked for. Please provide class names and line numbers if you desire otherwise.");
        } else if (!eventProcessor.getBreakpointContexts().isEmpty()) {
            writer.name("breakpoint");
            writer.beginArray();
            for (BreakPointContext bpc : eventProcessor.getBreakpointContexts()) {
                gson.toJson(bpc, BreakPointContext.class, writer);
            }
            writer.endArray();
            logger.info("Breakpoints serialised!");
        } else {
            writer.name("breakpoint").beginArray().endArray();
            logger.info("Output file was not generated as breakpoints were not encountered.");
        }

        if (skipReturnValues) {
            logger.info(
                    "Return data was not asked for. Please provide method names if you desire otherwise.");
        } else if (!eventProcessor.getReturnValues().isEmpty()) {
            writer.name("return");
            writer.beginArray();
            for (ReturnData rd : eventProcessor.getReturnValues()) {
                gson.toJson(rd, ReturnData.class, writer);
            }
            writer.endArray();
            logger.info("Return values serialised!");
        } else {
            writer.name("return").beginArray().endArray();
            logger.info("No method exits were encountered.");
        }
        writer.endObject();
        writer.close();
        logger.info("File output to: " + file.getAbsolutePath());
    }
}
