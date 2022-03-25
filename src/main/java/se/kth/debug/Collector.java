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

    @CommandLine.Option(names = "-t", description = "Path to test directory")
    private String pathToTestDirectory;

    @CommandLine.Option(names = "-o", description = "Path to output file (JSON)")
    private static String pathToOutputJson = "output.json";

    @CommandLine.Option(
            names = "--return-value-file",
            description = "File containing all return values of methods in the provided class.")
    private static String returnValueJson = "return.json";

    @CommandLine.Option(
            names = "-i",
            description = "File containing class names and breakpoints",
            defaultValue = "input.txt")
    private File classesAndBreakpoints;

    @CommandLine.Option(
            names = "--object-depth",
            description = "Nesting level of object required (default: ${DEFAULT-VALUE}).")
    private int objectDepth = 1;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Collector()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws IOException, AbsentInformationException {
        EventProcessor eventProcessor =
                new EventProcessor(providedClasspath, pathToTestDirectory, classesAndBreakpoints);
        eventProcessor.startEventProcessor(objectDepth);
        if (!eventProcessor.getBreakpointContexts().isEmpty()) {
            writeBreakpointsToFile(eventProcessor.getBreakpointContexts());
            logger.info("Output file generated!");
        } else {
            logger.info("Output file was not generated as breakpoints were not visited.");
        }
        if (!eventProcessor.getBreakpointContexts().isEmpty()) {
            writeReturnValuesToFile(eventProcessor.getReturnValues());
            logger.info("Return values are output to the file!");
        } else {
            logger.info("No method exits were encountered.");
        }
        return 0;
    }

    private static void writeBreakpointsToFile(List<BreakPointContext> breakPointContext)
            throws IOException {
        final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        try (FileWriter file = new FileWriter(pathToOutputJson)) {
            file.write(gson.toJson(breakPointContext));
        }
    }

    private static void writeReturnValuesToFile(List<ReturnData> returnValues) throws IOException {
        final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        try (FileWriter file = new FileWriter(returnValueJson)) {
            file.write(gson.toJson(returnValues));
        }
    }
}
