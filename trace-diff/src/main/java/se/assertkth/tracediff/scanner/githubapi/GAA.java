package se.assertkth.tracediff.scanner.githubapi;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

// GAM stands for: Github Api Adapter
public class GAA {
    private static final String TOKENS_PATH = System.getenv("TOKENS_PATH");
    private static int lastUsedToken = 0;

    public static GitHub g() throws IOException {
        GitHubBuilder githubBuilder = new GitHubBuilder();

        if (tokens != null && tokens.length > 0)
            githubBuilder = githubBuilder.withOAuthToken(tokens[lastUsedToken++ % tokens.length]);

        return githubBuilder.build();
    }

    static {
        try {
            tokens = FileUtils.readLines(new File(TOKENS_PATH), "UTF-8").toArray(new String[0]);
        } catch (IOException e) {
            new RuntimeException("No Github-API token file is provided. Repairnator will work without tokens.", e);
        }
    }

    private static String[] tokens;
}
