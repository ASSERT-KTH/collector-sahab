package io.github.chains_project.tracediff.scanner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import io.github.chains_project.tracediff.scanner.githubapi.code_changes.GithubAPIPullRequestAdapter;
import io.github.chains_project.tracediff.scanner.githubapi.code_changes.models.SelectedPullRequest;

public class PRScannerMain {
    public static void main(String[] args) throws IOException {
        long intervalStart = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30;
        List<SelectedPullRequest> selectedPRs =
                GithubAPIPullRequestAdapter.getInstance().getSingleLinePRs(intervalStart, 1000);

        FileUtils.writeLines(new File("files/SelectedPRs.txt"), selectedPRs);
    }
}
