package se.assertkth.tracediff.statediff.ui;

import com.github.gumtreediff.matchers.Mapping;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import se.assertkth.tracediff.Constants;
import se.assertkth.tracediff.models.SourceInfo;
import se.assertkth.tracediff.sharedutils.GHHelper;
import se.assertkth.tracediff.statediff.computer.StateDiffComputer;
import se.assertkth.tracediff.statediff.models.ProgramStateDiff;
import se.assertkth.tracediff.statediff.utils.ExecDiffHelper;
import spoon.reflect.code.CtReturn;

public class StateDiffUIManipulator {
    private static final Logger logger = Logger.getLogger(StateDiffUIManipulator.class.getName());
    private static File STATE_DIFF_WIDGET_TEMPLATE;
    //    private static final File STATE_DIFF_WIDGET_TEMPLATE =
    //        new
    // File("/home/khaes/phd/projects/explanation/code/Explainer/src/main/resources/state_diff/state_diff_widget.html");

    static {
        try {
            STATE_DIFF_WIDGET_TEMPLATE =
                    Files.createTempFile("", "state_diff_widget.html").toFile();
            FileUtils.copyInputStreamToFile(
                    StateDiffUIManipulator.class.getClassLoader().getResourceAsStream("state_diff_widget.html"),
                    STATE_DIFF_WIDGET_TEMPLATE);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create temp widget file.");
        }
    }

    public void addStateDiffToExecDiffUI(
            String slug,
            String commit,
            File leftReport,
            File rightReport,
            File srcFile,
            File dstFile,
            File ghFullDiff,
            String testsStr,
            String testLink,
            String outputPath)
            throws Exception {
        long processStartTime = new Date().getTime();
        boolean isHitDataIncluded = ghFullDiff != null;

        Map<Integer, Integer> returnSrcToDstMappings = new HashMap<>(), returnDstToSrcMappings = new HashMap<>();

        extractReturnMappings(srcFile, dstFile, returnSrcToDstMappings, returnDstToSrcMappings);

        SourceInfo srcInfo = new SourceInfo(srcFile), dstInfo = new SourceInfo(dstFile);

        if (ghFullDiff == null) {
            ghFullDiff = GHHelper.getGHDiff(slug, commit, srcInfo, dstInfo);
        }

        Pair<Map<Integer, Integer>, Map<Integer, Integer>> lineMappings =
                ExecDiffHelper.getMappingFromExecDiff(ghFullDiff, isHitDataIncluded);

        lineMappings.getLeft().putAll(returnSrcToDstMappings);
        lineMappings.getRight().putAll(returnDstToSrcMappings);

        logger.info("Line mappings and vars computed.");

        long endOfLineMappingAndVarComputation = new Date().getTime();

        logger.info("For commit " + commit + " line mappings and vars computation took "
                + (endOfLineMappingAndVarComputation - processStartTime) + " MILLIS");

        StateDiffComputer sdc = new StateDiffComputer(
                leftReport,
                rightReport,
                lineMappings.getLeft(),
                lineMappings.getRight(),
                srcInfo.getLineVars(),
                dstInfo.getLineVars(),
                Arrays.asList(testsStr.split(Constants.TEST_SEPARATOR)));

        ProgramStateDiff psd = sdc.computeProgramStateDiff();

        logger.info(psd.toString());

        long endOfDiffComputationTime = new Date().getTime();

        logger.info("For commit " + commit + " diff computation took "
                + (endOfDiffComputationTime - endOfLineMappingAndVarComputation) + " MILLIS");

        addStateDiffToExecDiffUI(psd, ghFullDiff, testLink, isHitDataIncluded);

        long endOfUIManipulationTime = new Date().getTime();

        logger.info("For commit " + commit + " UI manipulation took "
                + (endOfUIManipulationTime - endOfDiffComputationTime) + " MILLIS");

        File outputFile = new File(outputPath);
        outputFile.createNewFile();

        FileUtils.copyFile(ghFullDiff, outputFile);
    }

    private void extractReturnMappings(
            File srcFile,
            File dstFile,
            Map<Integer, Integer> returnSrcToDstMappings,
            Map<Integer, Integer> returnDstToSrcMappings)
            throws Exception {
        Diff diff = new AstComparator().compare(srcFile, dstFile);
        Iterator<Mapping> mappings = diff.getMappingsComp().iterator();
        while (mappings.hasNext()) {
            Mapping mapping = mappings.next();
            if (mapping.first.getMetadata("spoon_object") instanceof CtReturn
                    && mapping.second.getMetadata("spoon_object") instanceof CtReturn) {
                CtReturn srcElem = (CtReturn) mapping.first.getMetadata("spoon_object"),
                        dstElem = (CtReturn) mapping.second.getMetadata("spoon_object");
                int srcLine = srcElem.getPosition().getLine(),
                        dstLine = dstElem.getPosition().getLine();

                returnSrcToDstMappings.put(srcLine, dstLine);
                returnDstToSrcMappings.put(dstLine, srcLine);
            }
        }
    }

    private void addStateDiffToExecDiffUI(
            ProgramStateDiff stateDiff, File ghFullDiff, String testLink, boolean isHitDataIncluded) throws Exception {
        if (stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarVal() != null) {
            addStateDiffToExecDiffUI(
                    stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarVal(),
                    stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarValLine(),
                    "state",
                    false,
                    ghFullDiff,
                    stateDiff.getFirstPatchedUniqueStateSummary().getDifferencingTest(),
                    testLink,
                    isHitDataIncluded);
        }
        if (stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarVal() != null) {
            addStateDiffToExecDiffUI(
                    stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarVal(),
                    stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarValLine(),
                    "state",
                    true,
                    ghFullDiff,
                    stateDiff.getFirstOriginalUniqueStateSummary().getDifferencingTest(),
                    testLink,
                    isHitDataIncluded);
        }

        if (stateDiff.getPatchedUniqueReturn().getFirstUniqueVarVal() != null) {
            addStateDiffToExecDiffUI(
                    stateDiff.getPatchedUniqueReturn().getFirstUniqueVarVal(),
                    stateDiff.getPatchedUniqueReturn().getFirstUniqueVarValLine(),
                    "return",
                    false,
                    ghFullDiff,
                    stateDiff.getPatchedUniqueReturn().getDifferencingTest(),
                    testLink,
                    isHitDataIncluded);
        }
        if (stateDiff.getOriginalUniqueReturn().getFirstUniqueVarVal() != null) {
            addStateDiffToExecDiffUI(
                    stateDiff.getOriginalUniqueReturn().getFirstUniqueVarVal(),
                    stateDiff.getOriginalUniqueReturn().getFirstUniqueVarValLine(),
                    "return",
                    true,
                    ghFullDiff,
                    stateDiff.getOriginalUniqueReturn().getDifferencingTest(),
                    testLink,
                    isHitDataIncluded);
        }
    }

    private void addStateDiffToExecDiffUI(
            String diffStr,
            Integer diffLine,
            String diffType,
            boolean occursInOriginal,
            File ghFullDiff,
            String testName,
            String testLink,
            boolean isHitDataIncluded)
            throws Exception {
        String displayableTestName = testName.contains("::")
                ? testName.split("::")[1]
                : testName.split("\\.")[testName.split("\\.").length - 1];

        String stateDiffHtml = FileUtils.readFileToString(STATE_DIFF_WIDGET_TEMPLATE, "UTF-8");
        stateDiffHtml = stateDiffHtml
                .replace("{{line-num}}", diffLine.toString())
                .replace("{{diff-type}}", diffType)
                .replace("{{test-link}}", testLink)
                .replace("{{test-name}}", displayableTestName)
                .replace("{{unique-state}}", diffStr)
                .replace("{{unique-state-version}}", occursInOriginal ? "original" : "patched");
        ExecDiffHelper.addLineInfoAfter(diffLine, stateDiffHtml, ghFullDiff, occursInOriginal, isHitDataIncluded);
    }

    public static void main(String[] args) throws Exception {
        //        String leftReportPath = args[0], rightReportPath = args[1], leftSrcPath = args[2], rightSrcPath =
        // args[3],
        //                diffHtmlPath = args[4], testName = args[5], testLink = args[6];
        //        new StateDiffUIManipulator().addStateDiffToExecDiffUI(
        //                new File(leftReportPath),
        //                new File(rightReportPath),
        //                new File(leftSrcPath),
        //                new File(rightSrcPath),
        //                new File(diffHtmlPath),
        //                testName,
        //                testLink);

        new StateDiffUIManipulator()
                .addStateDiffToExecDiffUI(
                        "khaes-kth/commons-io",
                        "db23cdbcf787165df0aec971107f0136d830fcc3",
                        new File(
                                "/home/khaes/phd/projects/explanation/code/tmp/dspot-on-sorald/output/sahab-reports/4816441885916f66a16d8d2e21acc0e4fbd207cd/left.json"),
                        new File(
                                "/home/khaes/phd/projects/explanation/code/tmp/dspot-on-sorald/output/sahab-reports/4816441885916f66a16d8d2e21acc0e4fbd207cd/right.json"),
                        new File(
                                "/home/khaes/phd/projects/explanation/code/tmp/dspot-on-sorald/old-files/FastDateParser.java"),
                        new File(
                                "/home/khaes/phd/projects/explanation/code/tmp/dspot-on-sorald/new-files/FastDateParser.java"),
                        null,
                        "org.apache.commons.lang3.time.FastDateParserTest::testLang1380",
                        "http://example.com",
                        "/home/khaes/phd/projects/explanation/code/tmp/dspot-on-sorald/4816441885916f66a16d8d2e21acc0e4fbd207cd.html");
    }
}
