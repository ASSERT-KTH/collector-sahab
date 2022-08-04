package se.kth.debug;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import org.junit.platform.console.options.CommandLineOptions;
import org.junit.platform.console.tasks.ConsoleTestExecutor;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.MethodSelector;

public class JUnitTestRunner {
    public static void main(String... args) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Object> task =
                () -> {
                    runTests(args[0].split(" "));
                    return 0;
                };
        Future<Object> future = executor.submit(task);
        try {
            executor.shutdown();
            future.get(4, TimeUnit.MINUTES);
        } catch (Exception ex) {
            // handle other exceptions
            ex.printStackTrace();
        } finally {
            future.cancel(true);
        }
    }

    private static void runTests(String... tests) throws Exception {
        Set<MethodSelector> methods = new HashSet<>();
        Set<ClassSelector> classes = new HashSet<>();
        for (final String test : tests) {
            if (test.contains("::")) {
                String[] classAndMethod = test.split("::");
                String clazz = classAndMethod[0];
                String method = classAndMethod[1];

                methods.add(selectMethod(clazz + "#" + method));
            } else {
                classes.add(selectClass(test));
            }
        }
        CommandLineOptions options = new CommandLineOptions();
        options.setSelectedMethods(new ArrayList<>(methods));
        options.setSelectedClasses(new ArrayList<>(classes));
        options.setFailIfNoTests(true);
        options.setAnsiColorOutputDisabled(true);
        new ConsoleTestExecutor(options).execute(new PrintWriter(System.out));
    }
}
