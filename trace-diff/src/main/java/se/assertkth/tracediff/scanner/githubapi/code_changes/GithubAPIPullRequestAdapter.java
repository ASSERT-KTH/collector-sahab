package se.assertkth.tracediff.scanner.githubapi.code_changes;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.kohsuke.github.*;
import se.assertkth.tracediff.scanner.githubapi.GAA;
import se.assertkth.tracediff.scanner.githubapi.code_changes.models.FetchMode;
import se.assertkth.tracediff.scanner.githubapi.code_changes.models.SelectedPullRequest;
import se.assertkth.tracediff.scanner.githubapi.repositories.GithubAPIRepoAdapter;

public class GithubAPIPullRequestAdapter {

    private static GithubAPIPullRequestAdapter _instance;

    public static GithubAPIPullRequestAdapter getInstance() {
        if (_instance == null) {
            _instance = new GithubAPIPullRequestAdapter();
        }
        return _instance;
    }

    public List<SelectedPullRequest> getSingleLinePRs(
            GHRepository repo, long startDateForScanning, long since, boolean isFirstScan, FetchMode fetchMode)
            throws IOException {

        List<SelectedPullRequest> res = new ArrayList<>();

        // Search for all Pull Requests that are open
        GHPullRequestQueryBuilder query = repo.queryPullRequests().state(GHIssueState.OPEN);

        List<GHPullRequest> pullRequestsToAnalyze;

        List<GHPullRequest> allPullRequests = query.list().toList();
        pullRequestsToAnalyze = new ArrayList<>();

        if (isFirstScan) { // It means that all open pull requests created after startDateForScanning are considered
            allPullRequests.forEach(pullRequest -> {
                try {
                    if (pullRequest.getCreatedAt().getTime() >= startDateForScanning) {
                        pullRequestsToAnalyze.add(pullRequest);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else {
            allPullRequests.forEach(pullRequest -> {
                try {
                    List<GHIssueComment> comments = pullRequest.getComments();
                    List<GHPullRequestReviewComment> pullRequestReviewComments =
                            pullRequest.listReviewComments().toList();

                    boolean isUpdateToConsider = true;

                    if (pullRequest.getCreatedAt().getTime() >= startDateForScanning
                            && pullRequest.getUpdatedAt().getTime() >= since) {
                        // Avoid considering updates that are related to the addition of a new comment
                        if (!comments.isEmpty()) {
                            if (comments.get(comments.size() - 1).getUpdatedAt().compareTo(pullRequest.getUpdatedAt())
                                    == 0) {
                                isUpdateToConsider = false;
                            }
                        }
                        // Avoid considering updates that are related to the addition of a new review comment
                        if (isUpdateToConsider && !pullRequestReviewComments.isEmpty()) {
                            if (pullRequestReviewComments
                                            .get(pullRequestReviewComments.size() - 1)
                                            .getUpdatedAt()
                                            .compareTo(pullRequest.getUpdatedAt())
                                    == 0) {
                                isUpdateToConsider = false;
                            }
                        }

                        if (isUpdateToConsider) {
                            pullRequestsToAnalyze.add(pullRequest);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        for (GHPullRequest pullRequest : pullRequestsToAnalyze) {

            boolean isGithubPullRequestFailed = false;

            // It checks for pull requests whose head commit has a failure
            List<GHPullRequestCommitDetail> commits = pullRequest.listCommits().toList();
            String headCommitSHA = commits.get(commits.size() - 1).getSha();
            List<GHCommitStatus> statuses =
                    repo.getCommit(headCommitSHA).listStatuses().toList();

            List<GHCheckRun> checkRuns =
                    repo.getCommit(headCommitSHA).getCheckRuns().toList();

            // Check if a pull request has some failed checks
            for (int i = 0; i < checkRuns.size(); i++) {
                if (checkRuns.get(i) != null
                        && checkRuns.get(i).getConclusion() != null
                        && checkRuns.get(i).getConclusion().name().equalsIgnoreCase("FAILURE")) {
                    isGithubPullRequestFailed = true;
                    break;
                }
            }

            // Another check using the status of a commit instead of the check runs
            if (!isGithubPullRequestFailed) {
                for (int i = 0; i < statuses.size(); i++) {
                    if (statuses.get(i) != null
                            && statuses.get(i).getState() != null
                            && statuses.get(i).getState().name().equalsIgnoreCase("failure")) {
                        isGithubPullRequestFailed = true;
                        break;
                    }
                }
            }

            switch (fetchMode) {
                case ALL:
                    res.add(new SelectedPullRequest(
                            pullRequest.getId(),
                            pullRequest.getNumber(),
                            pullRequest.getUrl().toString(),
                            headCommitSHA,
                            pullRequest.getRepository().getFullName()));
                    break;
                case FAILED:
                    if (isGithubPullRequestFailed) {
                        res.add(new SelectedPullRequest(
                                pullRequest.getId(),
                                pullRequest.getNumber(),
                                pullRequest.getUrl().toString(),
                                headCommitSHA,
                                pullRequest.getRepository().getFullName()));
                    }
                    break;
            }
        }
        return res;
    }

    public List<SelectedPullRequest> getSingleLinePRs(
            long startDateForScanning,
            long intervalStart,
            boolean isFirstScan,
            FetchMode fetchMode,
            String fixedRepos) {

        List<SelectedPullRequest> selectedPullRequests = Collections.synchronizedList(new ArrayList<>());

        try {
            GHRepository repo = GAA.g().getRepository(fixedRepos);

            boolean isMaven = false;
            for (GHTreeEntry treeEntry : repo.getTree("HEAD").getTree()) {
                if (treeEntry.getPath().equals("pom.xml")) {
                    isMaven = true;
                    break;
                }
            }

            if (!isMaven) {
                return null;
            }

            selectedPullRequests.addAll(GithubAPIPullRequestAdapter.getInstance()
                    .getSingleLinePRs(repo, startDateForScanning, intervalStart, isFirstScan, fetchMode));

        } catch (Exception e) {
            System.err.println("error occurred for: " + fixedRepos);
            e.printStackTrace();
        }

        return selectedPullRequests;
    }

    public List<SelectedPullRequest> getSingleLinePRs(long intervalStart, int minStars) throws IOException {
        final Set<String> repositories = GithubAPIRepoAdapter.getInstance()
                .listJavaRepositories(intervalStart, minStars, GithubAPIRepoAdapter.MAX_STARS);

        AtomicInteger cnt = new AtomicInteger(0);
        List<SelectedPullRequest> selectedPullRequests = Collections.synchronizedList(new ArrayList<>());
        repositories.parallelStream().forEach(repoName -> {
            try {
                GHRepository repo = GAA.g().getRepository(repoName);
                System.out.println("Checking PRs for: " + repo.getName() + " " + cnt.incrementAndGet() + " from "
                        + repositories.size()
                        + " " + new Date(intervalStart));
                boolean isMaven = false, hasTestCI = false;
                for (GHTreeEntry treeEntry : repo.getTree("HEAD").getTree()) {
                    if (treeEntry.getPath().equals("pom.xml")) {
                        isMaven = true;
                    }
                    if (treeEntry.getPath().contains(".coveralls.yml")
                            || treeEntry.getPath().contains("codecov.yml")) {
                        hasTestCI = true;
                    }
                    if (isMaven && hasTestCI) break;
                }

                if (!isMaven || !hasTestCI) {
                    return;
                }

                selectedPullRequests.addAll(getSingleJavaLinePRs(repo, intervalStart));

            } catch (Exception e) {
                System.err.println("error occurred for: " + repoName);
                e.printStackTrace();
            }
        });

        return selectedPullRequests;
    }

    private Collection<? extends SelectedPullRequest> getSingleJavaLinePRs(GHRepository repo, long intervalStart)
            throws IOException {
        List<SelectedPullRequest> res = new ArrayList<>();

        List<GHPullRequest> pullRequests = repo.getPullRequests(GHIssueState.CLOSED);
        for (GHPullRequest pullRequest : pullRequests) {
            if (pullRequest.getCreatedAt().getTime() >= intervalStart) {
                List<GHPullRequestFileDetail> files = pullRequest.listFiles().toList();
                if (files.size() == 1) {
                    GHPullRequestFileDetail file = files.get(0);
                    if (file.getFilename().endsWith(".java") && file.getPatch().split("\\n").length == 1)
                        res.add(new SelectedPullRequest(pullRequest));
                }
            }
        }

        return res;
    }
}
