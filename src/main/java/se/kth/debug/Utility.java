package se.kth.debug;

import spoon.Launcher;
import spoon.reflect.CtModel;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Utility {
    private Utility() { }

    /**
     * Concatenates provided classpath with the system set classpath.
     *
     * @param classpath this classpath is usually the classpath of the
     *                  compiled project
     * @return concatenated string of classpath
     */
    public static String getFullClasspath(String classpath) throws MalformedURLException {
        String[] pathElements = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
        String[] providedClasspath = classpath.split(File.pathSeparator);

        Set<URI> classpathCollection = new HashSet<>();

        for (String s : providedClasspath) {
            URI uri = new File(s).toURI();
            classpathCollection.add(uri);
        }

        for (String pathElement : pathElements) {
            URI uri = new File(pathElement).toURI();
            classpathCollection.add(uri);
        }

        StringBuilder strClasspath = new StringBuilder();
        for (URI uri : classpathCollection) {
            if (uri == null) {
                continue;
            }
            File file = new File(uri.toURL().getFile());
            if (file.exists()) {
                strClasspath.append(file.getAbsolutePath()).append(File.pathSeparatorChar);
            }
        }

        return strClasspath.toString();
    }

    /**
     * Uses spoon to return fully-qualified names of all test classes.
     *
     * @param pathToTestDirectory test directory of project whose runtime
     *                            context needs to be collected
     * @return a string of test classes separated by " "
     */
    public static String getAllTests(String pathToTestDirectory) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(pathToTestDirectory);
        CtModel model = launcher.buildModel();

        List<String> fullyQualifiedNames = model.getAllTypes().stream()
                .map(type -> type.getPackage().toString() + "." + type.getSimpleName())
                .collect(Collectors.toList());
        return String.join(" ", fullyQualifiedNames);
    }
}
