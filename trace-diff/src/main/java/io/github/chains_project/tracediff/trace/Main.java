package io.github.chains_project.tracediff.trace;

import io.github.chains_project.tracediff.sharedutils.GHHelper;
import java.io.File;
import java.util.Arrays;

public class Main {
    public static void main(String args[]) throws Exception {
        String slug = args[0],
                commit = args[1],
                originalPath = args[2],
                patchedPath = args[3],
                outputDir = args[4],
                linkToFull = args[5],
                changeFile = args[6],
                selectedTest = args[7];

        new TraceAnalyzer()
                .generateTraceDiffsForGHChange(
                        slug,
                        commit,
                        new File(originalPath),
                        new File(patchedPath),
                        new File(outputDir),
                        linkToFull,
                        Arrays.asList(new String[] {changeFile}),
                        GHHelper.ChangeType.COMMIT,
                        selectedTest);
    }
}
