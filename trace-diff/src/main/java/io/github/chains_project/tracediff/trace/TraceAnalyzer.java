package io.github.chains_project.tracediff.trace;

import io.github.chains_project.tracediff.sharedutils.GHHelper;
import io.github.chains_project.tracediff.trace.models.GHReports;
import io.github.chains_project.tracediff.trace.models.LineMapping;
import io.github.chains_project.tracediff.trace.models.ReportConfig;
import io.github.chains_project.tracediff.trace.models.TraceInfo;
import io.github.chains_project.tracediff.trace.utils.CloverHelper;
import io.github.chains_project.tracediff.trace.utils.SpoonHelper;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Given the original and patched mvn projects, a {@link TraceAnalyzer} generates the execution trace diff.
 */
public class TraceAnalyzer {
    private static final String FINAL_REPORT_TEMPLATE_PATH = "/trace/final_report.html";
    private static final String FINAL_REPORT_DIR_PATH = "target/trace/";
    private static final String FINAL_REPORT_ORIGINAL_COL_ID = "original-trace";
    private static final String FINAL_REPORT_PATCHED_COL_ID = "patched-trace";
    private static final String FINAL_REPORT_CODE_KEYWORD = "{code}";
    private static final String FINAL_REPORT_COLOR_KEYWORD = "{color}";
    private static final String FINAL_REPORT_LINE_NUM_KEYWORD = "{line-num}";
    private static final String FINAL_REPORT_LINE_NUM_PLACEHOLDER = "            ";
    private static final String FINAL_REPORT_LINE_HIT_KEYWORD = "{line-hit}";
    private static final String FINAL_REPORT_LINE_HIT_PLACEHOLDER = "    ";
    private static final String FINAL_REPORT_LINE_TEMPLATE = FINAL_REPORT_LINE_NUM_KEYWORD + ":  "
            + "<span style=\"background-color: " + FINAL_REPORT_COLOR_KEYWORD + "\">"
            + FINAL_REPORT_LINE_HIT_KEYWORD + "  " + FINAL_REPORT_CODE_KEYWORD + "</span>";
    private static final String FINAL_GH_REPORT_FILENAME = "gh.html";
    private static final String FINAL_EXPANDED_GH_REPORT_FILENAME = "gh_full.html";
    private static final String INPUT_STR_SEPARATOR = ";";
    private static final String FINAL_REPORT_FILE_NAME_SEPARATOR = "-";
    private static final String JAVA_SUFFIX = ".java";
    private static final String HTML_SUFFIX = ".html";
    private static final int MAX_DEMONSTRABLE_HIT = 99;

    // does not use Spoon for line mapping, uses GH unified diff mapping instead
    public void generateTraceDiffsForGHCommit(
            String slug,
            String commit,
            File originalMvnDir,
            File patchedMvnDir,
            File outputDir,
            String expandedVersionLink,
            String selectedTest)
            throws Exception {
        List<String> modifiedFilePaths =
                GHHelper.cloneCommitAndGetChangedSources(slug, commit, originalMvnDir, patchedMvnDir);
        generateTraceDiffsForGHChange(
                slug,
                commit,
                originalMvnDir,
                patchedMvnDir,
                outputDir,
                expandedVersionLink,
                modifiedFilePaths,
                GHHelper.ChangeType.COMMIT,
                selectedTest);
    }

    // does not use Spoon for line mapping, uses GH unified diff mapping instead
    public void generateTraceDiffsForGHPR(
            String slug,
            String pr,
            File originalMvnDir,
            File patchedMvnDir,
            File outputDir,
            String expandedVersionLink,
            String selectedTest)
            throws Exception {
        List<String> modifiedFilePaths = GHHelper.clonePRAndGetChangedSources(slug, pr, originalMvnDir, patchedMvnDir);
        generateTraceDiffsForGHChange(
                slug,
                pr,
                originalMvnDir,
                patchedMvnDir,
                outputDir,
                expandedVersionLink,
                modifiedFilePaths,
                GHHelper.ChangeType.PR,
                selectedTest);
        return;
    }

    public void generateTraceDiffsForGHChange(
            String slug,
            String changeId,
            File originalMvnDir,
            File patchedMvnDir,
            File outputDir,
            String expandedVersionLink,
            List<String> modifiedFilePaths,
            GHHelper.ChangeType changeType,
            String selectedTest)
            throws Exception {
        if (modifiedFilePaths == null || modifiedFilePaths.isEmpty()) {
            System.out.println("Nothing printed because no execution diff exists for change: " + slug + "/" + changeId);
            return;
        }

        Map<String, Map<Integer, Integer>> originalCoverages =
                CloverHelper.getPerLineCoverages(originalMvnDir, modifiedFilePaths, selectedTest);
        Map<String, Map<Integer, Integer>> patchedCoverages =
                CloverHelper.getPerLineCoverages(patchedMvnDir, modifiedFilePaths, selectedTest);

        GHReports ghReports = GHHelper.getGHReports(
                slug,
                changeId,
                modifiedFilePaths,
                originalCoverages,
                patchedCoverages,
                expandedVersionLink,
                changeType);

        //        if (ghReports.getSummary().getLinesWithFewerExec() == 0 &&
        // ghReports.getSummary().getLinesWithMoreExec() == 0) {
        //            System.out.println("Nothing printed because no execution diff exists for change: " + slug + "/" +
        // changeId);
        //            return;
        //        }

        outputDir.mkdirs();
        File unexpandedReportFile =
                outputDir.toPath().resolve(FINAL_GH_REPORT_FILENAME).toFile();
        unexpandedReportFile.createNewFile();
        File expandedReportFile =
                outputDir.toPath().resolve(FINAL_EXPANDED_GH_REPORT_FILENAME).toFile();
        expandedReportFile.createNewFile();

        FileUtils.writeStringToFile(unexpandedReportFile, ghReports.getUnexpandedReport(), "UTF-8");
        FileUtils.writeStringToFile(expandedReportFile, ghReports.getExpandedReport(), "UTF-8");
    }

    public void generateTraceDiffs(
            File originalMvnDir,
            File patchedMvnDir,
            File outputDir,
            String modifiedFilePathsStr,
            String reportConfigsStr,
            String selectedTest)
            throws Exception {
        TraceInfo traceInfo = extractTraceInfo(originalMvnDir, patchedMvnDir, modifiedFilePathsStr, selectedTest);

        List<String> modifiedFilePaths = Arrays.asList(modifiedFilePathsStr.split(INPUT_STR_SEPARATOR));

        for (String path : modifiedFilePaths)
            for (String configStr : reportConfigsStr.split(INPUT_STR_SEPARATOR))
                outputTraceDiff(
                        originalMvnDir,
                        patchedMvnDir,
                        outputDir,
                        Path.of(path),
                        traceInfo.getPathToLineMapping().get(path),
                        traceInfo.getPathToOriginalCoverage().get(path),
                        traceInfo.getPathToPatchedCoverage().get(path),
                        new ReportConfig(configStr));
    }

    private TraceInfo extractTraceInfo(
            File originalMvnDir, File patchedMvnDir, String modifiedFilePathsStr, String selectedTest)
            throws Exception {
        if (modifiedFilePathsStr.isEmpty()) return null;

        List<String> modifiedFilePaths = Arrays.asList(modifiedFilePathsStr.split(INPUT_STR_SEPARATOR));
        Map<String, LineMapping> filePathToLineMapping = new HashMap<>();
        for (String path : modifiedFilePaths)
            filePathToLineMapping.put(
                    path,
                    SpoonHelper.getLineMapping(
                            originalMvnDir.toPath().resolve(path).toFile(),
                            patchedMvnDir.toPath().resolve(path).toFile()));

        Map<String, Map<Integer, Integer>> originalCoverages =
                CloverHelper.getPerLineCoverages(originalMvnDir, modifiedFilePaths, selectedTest);
        Map<String, Map<Integer, Integer>> patchedCoverages =
                CloverHelper.getPerLineCoverages(patchedMvnDir, modifiedFilePaths, selectedTest);

        return new TraceInfo(filePathToLineMapping, originalCoverages, patchedCoverages);
    }

    private void outputTraceDiff(
            File originalMvnDir,
            File patchedMvnDir,
            File outputDir,
            Path modifiedFilePath,
            LineMapping lineMapping,
            Map<Integer, Integer> originalCoverage,
            Map<Integer, Integer> patchedCoverage,
            ReportConfig reportConfig)
            throws IOException, URISyntaxException {
        List<String>
                originalLines =
                        FileUtils.readLines(
                                originalMvnDir
                                        .toPath()
                                        .resolve(modifiedFilePath)
                                        .toFile(),
                                "UTF-8"),
                patchedLines =
                        FileUtils.readLines(
                                patchedMvnDir.toPath().resolve(modifiedFilePath).toFile(), "UTF-8");

        String finalReportTemplate = new Scanner(
                        CloverHelper.class.getResourceAsStream(FINAL_REPORT_TEMPLATE_PATH), "UTF-8")
                .useDelimiter("\\A")
                .next();
        Document reportDoc = Jsoup.parse(finalReportTemplate);

        Element originalCol = reportDoc.getElementById(FINAL_REPORT_ORIGINAL_COL_ID),
                patchedCol = reportDoc.getElementById(FINAL_REPORT_PATCHED_COL_ID);

        fillReportColumn(
                lineMapping.getSrcToDst(),
                lineMapping.getSrcNewLines(),
                originalCoverage,
                patchedCoverage,
                originalLines,
                originalCol,
                reportConfig,
                true);
        fillReportColumn(
                lineMapping.getDstToSrc(),
                lineMapping.getDstNewLines(),
                patchedCoverage,
                originalCoverage,
                patchedLines,
                patchedCol,
                reportConfig,
                false);

        File finalReportFile = new File(Path.of(
                        outputDir.getPath(),
                        FINAL_REPORT_DIR_PATH,
                        reportConfig.toString()
                                + FINAL_REPORT_FILE_NAME_SEPARATOR
                                + modifiedFilePath.toString().replace(File.separator, FINAL_REPORT_FILE_NAME_SEPARATOR))
                .toString()
                .replace(JAVA_SUFFIX, HTML_SUFFIX));
        finalReportFile.getParentFile().mkdirs();
        finalReportFile.createNewFile();
        String finalHtml = trimCodeString(reportDoc.toString());
        FileUtils.writeStringToFile(finalReportFile, finalHtml, "UTF-8");
    }

    private String trimCodeString(String originalStr) {
        // TODO: clean it!
        String[] parts = originalStr.split("trace\">");
        String result = "";
        for (int i = 0; i < parts.length - 1; i++) {
            result += (parts[i].startsWith("\n            ") ? parts[i].replaceFirst("\n            ", "") : parts[i])
                    + "trace\">";
        }
        result += parts[parts.length - 1].replaceFirst("\n            ", "");
        return result;
    }

    private void fillReportColumn(
            Map<Integer, Integer> lineNumberMapping,
            Set<Integer> newLines,
            Map<Integer, Integer> sourceCoverage,
            Map<Integer, Integer> targetCoverage,
            List<String> lines,
            Element colElem,
            ReportConfig reportConfig,
            boolean isSrc) {
        for (int i = 0; i < lines.size(); i++) {
            int sourceLineNumber = i + 1;
            int targetLineNumber =
                    lineNumberMapping.containsKey(sourceLineNumber) ? lineNumberMapping.get(sourceLineNumber) : -1;

            int srcHitCnt = sourceCoverage.containsKey(sourceLineNumber) ? sourceCoverage.get(sourceLineNumber) : -1;
            String srcHitCntStr = srcHitCnt >= 0
                    ? (srcHitCnt <= MAX_DEMONSTRABLE_HIT ? srcHitCnt : MAX_DEMONSTRABLE_HIT + "+") + ""
                    : "";

            String lineNumStr = !isSrc
                    ? (sourceLineNumber + "->" + (targetLineNumber >= 0 ? targetLineNumber : "N"))
                    : sourceLineNumber + "";
            String reportLine = FINAL_REPORT_LINE_TEMPLATE
                    .replace(FINAL_REPORT_CODE_KEYWORD, lines.get(i))
                    .replace(
                            FINAL_REPORT_LINE_NUM_KEYWORD,
                            FINAL_REPORT_LINE_NUM_PLACEHOLDER.substring(lineNumStr.length()) + lineNumStr)
                    .replace(
                            FINAL_REPORT_LINE_HIT_KEYWORD,
                            reportConfig.isShowHits()
                                    ? FINAL_REPORT_LINE_HIT_PLACEHOLDER.substring(srcHitCntStr.length()) + srcHitCntStr
                                    : FINAL_REPORT_LINE_HIT_PLACEHOLDER);

            String backgroundColor = "white";
            if (lineNumberMapping.containsKey(sourceLineNumber) && (reportConfig.isAllColors() || !isSrc)) {
                int targetHitCnt =
                        targetCoverage.containsKey(targetLineNumber) ? targetCoverage.get(targetLineNumber) : -1;
                backgroundColor = srcHitCnt == targetHitCnt ? "white" : srcHitCnt > targetHitCnt ? "cyan" : "red";
            } else if (newLines.contains(sourceLineNumber)) {
                backgroundColor = "yellow";
                if (!isSrc) reportLine = reportLine.replaceFirst("N", "U"); // change the mapped line sign from N to U
            }
            reportLine = reportLine.replace(FINAL_REPORT_COLOR_KEYWORD, backgroundColor);

            colElem.append(reportLine);
            colElem.append("<br>");
        }
    }

    public static void main(String[] args) throws Exception {
        //        File sample1 = new File("/home/khaes/phd/projects/explanation/code/tmp/jtar");
        //        File sample2 = new File("/home/khaes/phd/projects/explanation/code/tmp/jtar2");
        //        new TraceAnalyzer(sample1, sample2).generatedTraceDiff(sample2,
        // Path.of("src/main/java/org/kamranzafar/jtar/TarHeader.java"));

        //        File sample1 = new File("/home/khaes/phd/projects/explanation/code/tmp/swagger-dubbo2");
        //        File sample2 = new File("/home/khaes/phd/projects/explanation/code/tmp/swagger-dubbo");
        //        new TraceAnalyzer()
        //                .generateTraceDiffs(sample1, sample2, sample2,
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/annotations/EnableDubboSwagger.java;" +
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/config/DubboPropertyConfig.java;" +
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/config/DubboServiceScanner.java;" +
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/config/SwaggerDocCache.java;" +
        //                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/http/HttpMatch.java;"
        // +
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/http/ReferenceManager.java;" +
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/DubboReaderExtension.java;" +
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/NameDiscover.java;" +
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/DubboReaderExtension.java;" +
        //                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/Reader.java;" +
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/ReaderContext.java;" +
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/ReaderExtension.java;" +
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/web/DubboHttpController.java;" +
        //
        // "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/web/SwaggerDubboController.java;",
        //                        "showHits-allColors;showHits;allColors; ");

        File original = new File("/home/khaes/phd/projects/explanation/code/tmp/original");
        File patched = new File("/home/khaes/phd/projects/explanation/code/tmp/patched");
        File outputDir = new File("/home/khaes/phd/projects/explanation/code/tmp/patched/target/trace");
        //        new TraceAnalyzer()
        //                .generateTraceDiffsForGHCommit("brianfrankcooper/YCSB",
        //                        "0a43104985bb919cd4ffcc9e1c284e4a564d81cc",
        //                        original, patched, outputDir, "http://example.com");
        //        new TraceAnalyzer()
        //                .generateTraceDiffsForGHCommit("khaes-kth/drr-as-pr",
        //                        "9e7c73de8437cfaf52671dd0adc7ab80a335a8f2",
        //                        original, patched, outputDir, "http://example.com");

        //        new TraceAnalyzer()
        //                .generateTraceDiffsForGHPR("kungfoo/geohash-java",
        //                        "45",
        //                        original, patched, outputDir, "http://example.com");

        new TraceAnalyzer()
                .generateTraceDiffsForGHChange(
                        "khaes-kth/drr-execdiff",
                        "9c2d18b38dd7df29612bb9888c59d8cf262a7977",
                        original,
                        patched,
                        outputDir,
                        "http://example.com",
                        Arrays.asList(new String[] {
                            "Math-2/src/main/java/org/apache/commons/math3/distribution/AbstractIntegerDistribution.java"
                        }),
                        GHHelper.ChangeType.COMMIT,
                        "org.apache.commons.math3.distribution.HypergeometricDistributionTest::testMath1021");
    }
}
