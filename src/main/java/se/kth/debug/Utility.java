package se.kth.debug;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import spoon.Launcher;
import spoon.reflect.CtModel;

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
     * Uses spoon to return fully-qualified names of all test classes.
     *
     * @param pathToTestDirectory test directory of project whose runtime context needs to be
     *     collected
     * @return a string of test classes separated by " "
     */
    public static String getAllTests(String pathToTestDirectory) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(pathToTestDirectory);
        CtModel model = launcher.buildModel();

        List<String> fullyQualifiedNames =
                model.getAllTypes().stream()
                        .map(
                                type ->
                                        String.format(
                                                "%s.%s",
                                                type.getPackage().toString(), type.getSimpleName()))
                        .collect(Collectors.toList());
        return String.join(" ", fullyQualifiedNames);
    }
}
