package se.kth.debug;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.concurrent.*;
import org.junit.platform.console.options.CommandLineOptions;
import org.junit.platform.console.tasks.ConsoleTestExecutor;

public class MethodTestRunner {
    public static void main(String... args) {
        String[] tests = args[0].split(" ");
        for (final String test : tests) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<Object> task =
                    () -> {
                        runTest(test);
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

    private static void runTest(String test) throws Exception {
        CommandLineOptions options = new CommandLineOptions();
        options.setSelectedClasses(Collections.singletonList(test));
        options.setFailIfNoTests(true);
        new ConsoleTestExecutor(options).execute(new PrintWriter(System.out));
    }
}
