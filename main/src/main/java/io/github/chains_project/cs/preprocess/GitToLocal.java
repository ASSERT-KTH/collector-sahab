package io.github.chains_project.cs.preprocess;

import io.github.chains_project.cs.commons.Pair;
import io.github.chains_project.cs.commons.Revision;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitToLocal {
    public static Pair<Revision, Revision> getRevisions(Path project, String leftHash, String rightHash)
            throws IOException, InterruptedException {
        try {
            String hashMix = leftHash.substring(0, 7) + "_" + rightHash.substring(0, 7) + "_";
            String prefix = project.getFileName().toString() + "_" + hashMix;
            Path tempDirectory = Files.createTempDirectory(prefix);
            return new Pair<>(
                    copy(project, tempDirectory.resolve("left"), leftHash),
                    copy(project, tempDirectory.resolve("right"), rightHash));
        } catch (StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Hash should be at least 7 characters long.");
        }
    }

    private static int checkout(Path cwd, String commit) throws IOException, InterruptedException {
        ProcessBuilder checkoutBuilder = new ProcessBuilder("git", "checkout", commit);
        checkoutBuilder.directory(cwd.toFile());
        Process p = checkoutBuilder.start();
        return p.waitFor();
    }

    private static Revision copy(Path originalProjectDirectory, Path destinationDirectory, String commit)
            throws IOException, InterruptedException {
        if (checkout(originalProjectDirectory, commit) != 0) {
            throw new RuntimeException("Could not checkout " + commit);
        }

        ProcessBuilder cpBuilder = new ProcessBuilder(
                "cp",
                "-r",
                originalProjectDirectory.toAbsolutePath().toString(),
                destinationDirectory.toAbsolutePath().toString());
        cpBuilder.directory(originalProjectDirectory.getParent().toFile());
        Process p = cpBuilder.start();
        p.waitFor();

        if (deleteGitHistory(destinationDirectory) != 0) {
            throw new RuntimeException("Could not delete git history " + commit);
        }

        return new Revision(destinationDirectory, commit);
    }

    private static int deleteGitHistory(Path project) throws IOException, InterruptedException {
        ProcessBuilder rmBuilder = new ProcessBuilder("rm", "-rf", ".git");
        rmBuilder.directory(project.toFile());
        Process p = rmBuilder.start();
        return p.waitFor();
    }
}
