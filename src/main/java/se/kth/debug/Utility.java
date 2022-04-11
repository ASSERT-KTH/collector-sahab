package se.kth.debug;

import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

public class Utility {
    private static final Logger logger = Logger.getLogger("Utility");

    private Utility() {}

    /**
     * Concatenates provided classpath with the system set classpath.
     *
     * @param providedClasspath usually the classpath of the compiled project, its tests, and
     *     dependencies
     * @return concatenated string of classpath
     */
    public static String getClasspathForRunningJUnit(String[] providedClasspath) {
        Set<String> classpathCollection = new HashSet<>();

        String[] pathElements =
                System.getProperty("java.class.path").split(System.getProperty("path.separator"));

        classpathCollection.addAll(verifyAndGetClasspath(pathElements));
        classpathCollection.addAll(verifyAndGetClasspath(providedClasspath));

        return String.join(":", classpathCollection);
    }

    private static Set<String> verifyAndGetClasspath(String[] classpath) {
        Set<String> classpathCollection = new HashSet<>();
        for (String cp : classpath) {
            URI uri = new File(cp).toURI();
            try {
                File file = new File(uri.toURL().getFile());
                if (file.exists()) {
                    classpathCollection.add(file.getAbsolutePath());
                }
            } catch (MalformedURLException e) {
                logger.warning(cp + " is not a valid path");
            }
        }
        return classpathCollection;
    }

    /**
     * Returns test separated by space. This is done so that split function can be called correctly.
     *
     * @param tests array of tests
     * @return a string of test classes separated by " "
     */
    public static String parseTests(String[] tests) {

        return String.join(" ", tests);
    }

    /** Checks if at least one method has `@Test` annotation. */
    private static boolean hasAtLeastOneJUnitTestMethod(CtType<?> testCase) {
        Set<CtMethod<?>> testMethods = testCase.getMethods();
        List<CtMethod<?>> annotatedTestMethods =
                testMethods.stream()
                        .filter(
                                ctMethod -> {
                                    List<CtAnnotation<?>> annotations = ctMethod.getAnnotations();
                                    for (CtAnnotation<?> annotation : annotations) {
                                        if (annotation.getName().contains("Test")) {
                                            return true;
                                        }
                                    }
                                    return false;
                                })
                        .collect(Collectors.toList());

        return annotatedTestMethods.size() > 0;
    }

    /**
     * Overriding callable to retain the unique reference to the value so that it can be replaced
     * later.
     */
    public static class CallableWithIDOfValue implements Callable<Value> {
        private final ThreadReference thread;
        private final Value value;

        CallableWithIDOfValue(ThreadReference thread, Value value) {
            this.thread = thread;
            this.value = value;
        }

        @Override
        public Value call() throws Exception {
            Method toArray =
                    ((ObjectReference) value).referenceType().methodsByName("toArray").get(0);
            return ((ObjectReference) value).invokeMethod(thread, toArray, List.of(), 0);
        }

        public Long getId() {
            return ((ObjectReference) value).uniqueID();
        }
    }
}