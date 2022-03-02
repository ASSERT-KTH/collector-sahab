package se.kth.debug;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class JavaUtils {
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

        StringBuilder strClasspath = new StringBuilder("");
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
}
