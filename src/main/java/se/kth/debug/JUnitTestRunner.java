package se.kth.debug;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.concurrent.*;
import org.junit.platform.console.options.CommandLineOptions;
import org.junit.platform.console.tasks.ConsoleTestExecutor;

public class JUnitTestRunner {
    public static void main(String... args) {
        String[] tests = args[0].split(" ");
        for (final String test : tests) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<Object> task =
                    () -> {
                        if (test.contains("::")) {
                            runTestMethod(test);
                        } else {
                            runTestClass(test);
                        }
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
    }

    private static void runTestMethod(String test) throws Exception {
        String[] classAndMethod = test.split("::");
        String clazz = classAndMethod[0];
        String method = classAndMethod[1];

        CommandLineOptions options = new CommandLineOptions();
        options.setSelectedMethods(Collections.singletonList(selectMethod(clazz + "#" + method)));
        options.setFailIfNoTests(true);
        options.setAnsiColorOutputDisabled(true);
        new ConsoleTestExecutor(options).execute(new PrintWriter(System.out));
    }

    private static void runTestClass(String test) throws Exception {
        CommandLineOptions options = new CommandLineOptions();
        options.setSelectedClasses(Collections.singletonList(selectClass(test)));
        options.setFailIfNoTests(true);
        options.setAnsiColorOutputDisabled(true);
        new ConsoleTestExecutor(options).execute(new PrintWriter(System.out));
    }
}
