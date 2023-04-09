package se.assertkth.cs.commons;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Revision {
    private final Path path;
    private final String hash;

    public Revision(Path path, String hash) {
        this.path = path;
        this.hash = hash;
    }

    public Path getPath() {
        return path;
    }

    public String getHash() {
        return hash;
    }

    public Path resolveFilename(String filename) throws FileNotFoundException {
        Path absolutePath = Paths.get(path.toAbsolutePath().toUri()).resolve(filename);

        if (!absolutePath.toFile().exists()) {
            throw new FileNotFoundException(filename + " does not exist in " + path.toAbsolutePath());
        }
        return absolutePath;
    }
}
