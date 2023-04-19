package io.github.chains_project.tracediff.sharedutils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.chains_project.tracediff.models.CodeInterval;
import io.github.chains_project.tracediff.models.SourceInfo;
import io.github.chains_project.tracediff.trace.models.GHReports;
import io.github.chains_project.tracediff.trace.utils.PH;

public class GHHelper {
    private static final String GH_COMMIT_KEYWORD = "{commit}";
    private static final String GH_SLUG_KEYWORD = "{slug}";
    private static final String GH_PR_KEYWORD = "{pr}";
    private static final String GH_PR_URL_TEMPLATE =
            "https://github.com/" + GH_SLUG_KEYWORD + "/pull/" + GH_PR_KEYWORD + "/files";
    private static final String GH_COMMIT_URL_TEMPLATE =
            "https://github.com/" + GH_SLUG_KEYWORD + "/commit/" + GH_COMMIT_KEYWORD;
    private static final String GH_PR_REF_TEMPLATE = "pull/" + GH_PR_KEYWORD + "/head";
    private static final String GH_REPO_URL_TEMPLATE = "https://github.com/" + GH_SLUG_KEYWORD + ".git";
    private static final String GIT_PREVIOUS_COMMIT_REF = "HEAD~1";

    private static final String JAVA_FILE_EXTENSION = ".java";
    private static final String TEST_FILE_SUB_STR = "/test/";
    private static final long SELENIUM_LOAD_WAIT_SEC = 1000L;

    public static List<String> cloneCommitAndGetChangedSources(
            String slug, String commit, File originalDir, File patchedDir) throws Exception {
        originalDir.mkdirs();
        patchedDir.mkdirs();

        String repoGitUrl = GH_REPO_URL_TEMPLATE.replace(GH_SLUG_KEYWORD, slug);
        int cloneRes = PH.run(originalDir, "cloning " + slug, "git", "clone", repoGitUrl, ".");
        if (cloneRes != 0) throw new Exception("Cannot clone " + slug);

        int checkoutRes = PH.run(originalDir, "checkout to " + commit, "git", "checkout", commit);
        if (checkoutRes != 0) throw new Exception("Cannot checkout " + commit);

        FileUtils.copyDirectory(originalDir, patchedDir);

        checkoutRes = PH.run(originalDir, "checkout to " + commit, "git", "checkout", GIT_PREVIOUS_COMMIT_REF);
        if (checkoutRes != 0) throw new Exception("Cannot checkout to the previous commit of" + commit);

        List<String> modifiedSrcPaths = new ArrayList<>();

        String commitUrl = getCommitUrl(slug, commit);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("headless");
        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get(commitUrl);
            Thread.sleep(SELENIUM_LOAD_WAIT_SEC);

            List<WebElement> changedFileElems = driver.findElements(By.cssSelector("a.Link--primary"));
            for (WebElement changedFileElem : changedFileElems) {
                String filePath = changedFileElem.getText();
                if (filePath != null && filePath.endsWith(JAVA_FILE_EXTENSION)) modifiedSrcPaths.add(filePath);
            }
        } finally {
            return modifiedSrcPaths;
        }
    }

    public static List<String> clonePRAndGetChangedSources(String slug, String pr, File originalDir, File patchedDir)
            throws Exception {
        int commitCnt = 0;
        List<String> modifiedSrcPaths = new ArrayList<>();

        String prUrl = GH_PR_URL_TEMPLATE.replace(GH_SLUG_KEYWORD, slug).replace(GH_PR_KEYWORD, pr);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get(prUrl);
            Thread.sleep(SELENIUM_LOAD_WAIT_SEC);

            List<WebElement> changedFileElems = driver.findElements(By.cssSelector("a.Link--primary"));
            for (WebElement changedFileElem : changedFileElems) {
                String filePath = changedFileElem.getText();
                if (filePath != null && filePath.endsWith(JAVA_FILE_EXTENSION)) modifiedSrcPaths.add(filePath);
            }

            WebElement commitsButton = driver.findElement(By.cssSelector("[href$=\"commits\"]"));
            commitsButton.click();
            Thread.sleep(SELENIUM_LOAD_WAIT_SEC);

            commitCnt = driver.findElements(By.cssSelector("[aria-label=\"View commit details\"]"))
                    .size();
        } catch (Exception e) {
            throw new Exception("Cannot load the PR page.");
        }

        originalDir.mkdirs();
        patchedDir.mkdirs();

        String repoGitUrl = GH_REPO_URL_TEMPLATE.replace(GH_SLUG_KEYWORD, slug);
        int cloneRes = PH.run(patchedDir, "cloning " + slug, "git", "clone", repoGitUrl, ".");
        if (cloneRes != 0) throw new Exception("Cannot clone " + slug);

        int checkoutRes = PH.run(
                patchedDir,
                "(1) checkout to pr " + pr,
                "git",
                "fetch",
                "origin",
                GH_PR_REF_TEMPLATE.replace(GH_PR_KEYWORD, pr) + ":explainer-tmp");
        if (checkoutRes != 0) throw new Exception("Cannot checkout pr " + pr);

        checkoutRes = PH.run(patchedDir, "(2) checkout to pr " + pr, "git", "checkout", "explainer-tmp");
        if (checkoutRes != 0) throw new Exception("Cannot checkout to pr " + pr);

        FileUtils.copyDirectory(patchedDir, originalDir);

        checkoutRes =
                PH.run(originalDir, "checkout to prevoius commit of pr " + pr, "git", "checkout", "HEAD~" + commitCnt);
        if (checkoutRes != 0) throw new Exception("Cannot checkout to previous version of pr " + pr);

        return modifiedSrcPaths;
    }

    public static GHReports getGHReports(
            String slug,
            String id,
            List<String> modifiedFiles,
            Map<String, Map<Integer, Integer>> originalCoverages,
            Map<String, Map<Integer, Integer>> patchedCoverages,
            String linkToExpanded,
            ChangeType changeType) {
        String pageUrl = null;
        switch (changeType) {
            case COMMIT:
                pageUrl = getCommitUrl(slug, id);
                break;
            case PR:
                pageUrl = GH_PR_URL_TEMPLATE.replace(GH_SLUG_KEYWORD, slug).replace(GH_PR_KEYWORD, pageUrl);
                break;
        }

        return getGhReportsForDiffPage(modifiedFiles, originalCoverages, patchedCoverages, linkToExpanded, pageUrl);
    }

    private static String getCommitUrl(String slug, String id) {
        return GH_COMMIT_URL_TEMPLATE.replace(GH_SLUG_KEYWORD, slug).replace(GH_COMMIT_KEYWORD, id);
    }

    private static File saveGhDiffInTempFile(String diffPageUrl) throws InterruptedException, IOException {
        File diffFile = Files.createTempFile("", "gh_report.html").toFile();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("headless");
        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get(diffPageUrl);
            Thread.sleep(SELENIUM_LOAD_WAIT_SEC);

            JavascriptExecutor jse = ((JavascriptExecutor) driver);

            while (true) {
                List<WebElement> expandableElems = driver.findElements(By.cssSelector("a.js-expand"));
                if (expandableElems == null || expandableElems.isEmpty()) break;

                expandableElems.get(0).click();
                Thread.sleep(SELENIUM_LOAD_WAIT_SEC);
            }

            // removing highlights for expanded lines
            jse.executeScript("Array.from(document.getElementsByClassName('blob-expanded'))"
                    + ".forEach(e => e.classList.remove('blob-expanded'))");

            FileUtils.writeStringToFile(diffFile, driver.getPageSource(), "UTF-8");

        } catch (Exception e) {
            throw new Exception("Could not get the GH page.");
        } finally {
            driver.quit();
            return diffFile;
        }
    }

    private static GHReports getGhReportsForDiffPage(
            List<String> modifiedFiles,
            Map<String, Map<Integer, Integer>> originalCoverages,
            Map<String, Map<Integer, Integer>> patchedCoverages,
            String linkToExpanded,
            String diffPageUrl) {
        String unexpandedHTML = null, expandedHTML = null;
        GHReports.ReportSummary reportSummary = null;

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("headless");
        WebDriver nonExpandingDriver = new ChromeDriver(options);
        WebDriver expandingDriver = new ChromeDriver(options);
        try {
            // adding trace data to both expanding and non-expanding versions
            expandingDriver.get(diffPageUrl);
            Thread.sleep(SELENIUM_LOAD_WAIT_SEC);
            reportSummary =
                    addTraceData(expandingDriver, modifiedFiles, originalCoverages, patchedCoverages, true, null);

            nonExpandingDriver.get(diffPageUrl);
            Thread.sleep(SELENIUM_LOAD_WAIT_SEC);
            addTraceData(nonExpandingDriver, modifiedFiles, originalCoverages, patchedCoverages, false, reportSummary);

            // adding link to expanded version in the unexpanded version
            unexpandedHTML = nonExpandingDriver.getPageSource();
            expandedHTML = expandingDriver.getPageSource();
            boolean showExpandWarning = expandedContainsExecDiff(nonExpandingDriver, expandingDriver);
            addLinkToExpandedVersion(nonExpandingDriver, expandingDriver, linkToExpanded, showExpandWarning);

            unexpandedHTML = nonExpandingDriver.getPageSource();
            expandedHTML = expandingDriver.getPageSource();
        } catch (Exception e) {
            throw new Exception("Could not get mappings from GH page.");
        } finally {
            nonExpandingDriver.quit();
            expandingDriver.quit();

            return new GHReports(expandedHTML, unexpandedHTML, reportSummary);
        }
    }

    // returns the summary span's html
    private static String addExecDiffSummary(WebDriver driver, WebElement diffElem, GHReports.ReportSummary summary) {
        String path = diffElem.getAttribute("data-path"), summaryHTML = null;
        if (summary != null
                && summary.getPathToSummaryHTML() != null
                && summary.getPathToSummaryHTML().containsKey(path))
            summaryHTML = summary.getPathToSummaryHTML().get(path);
        else {
            int redSlots = summary.getLinesWithFewerExec() > 0 ? 1 : 0,
                    greenSlots = summary.getLinesWithMoreExec() > 0 ? 1 : 0,
                    graySlots = summary.getLinesWithEqualExec() > 0 ? 1 : 0;

            if (summary.getLinesWithMoreExec() < summary.getLinesWithFewerExec()) redSlots = 5 - greenSlots - graySlots;
            else if (summary.getLinesWithFewerExec() < summary.getLinesWithMoreExec())
                greenSlots = 5 - redSlots - graySlots;
            else {
                redSlots = greenSlots = 2;
                graySlots = 1;
            }

            List<String> summaryLabelClasses = new ArrayList<>();
            summaryLabelClasses.addAll(
                    Collections.nCopies(redSlots, "<span class=\\\"diffstat-block-deleted\\\"></span>"));
            summaryLabelClasses.addAll(
                    Collections.nCopies(greenSlots, "<span class=\\\"diffstat-block-added\\\"></span>"));
            summaryLabelClasses.addAll(
                    Collections.nCopies(graySlots, "<span class=\\\"diffstat-block-neutral\\\"></span>"));

            summaryHTML =
                    ("EXEC-DIFF <br> <span class=\\\"diffstat tooltipped tooltipped-e\\\" aria-label=\\\"{more-exec} lines executed "
                                    + "more &amp; {fewer-exec} lines executed fewer times.\\\"><span style=\\\"margin-right: 5px\\\">{total-changed}</span>"
                                    + "{spans}</span>")
                            .replace("{more-exec}", summary.getLinesWithMoreExec() + "")
                            .replace("{fewer-exec}", summary.getLinesWithFewerExec() + "")
                            .replace("{equal-exec}", summary.getLinesWithEqualExec() + "")
                            .replace("{spans}", StringUtils.join(summaryLabelClasses, ""))
                            .replace(
                                    "{total-changed}",
                                    (summary.getLinesWithFewerExec() + summary.getLinesWithMoreExec()) + "");
        }

        //        ((JavascriptExecutor) driver)
        //
        // .executeScript(("arguments[0].querySelector(\"details.js-file-header-dropdown\").parentNode.innerHTML =
        // \"{new-html}\"")
        //                        .replace("{new-html}", summaryHTML), diffElem);
        ((JavascriptExecutor) driver)
                .executeScript(
                        ("arguments[0].parentNode.querySelector(\"td\").innerHTML = \"{new-html}\"")
                                .replace("{new-html}", summaryHTML),
                        diffElem);

        return summaryHTML;
    }

    // returns if there is some execution trace diff
    private static GHReports.ReportSummary addTraceData(
            WebDriver driver,
            List<String> modifiedFiles,
            Map<String, Map<Integer, Integer>> originalCoverages,
            Map<String, Map<Integer, Integer>> patchedCoverages,
            boolean expand,
            GHReports.ReportSummary reportSummary)
            throws InterruptedException {
        int totalLinesWithMoreExec = 0, totalLinesWithFewerExec = 0, totalLinesWithEqualExec = 0;
        Map<String, String> pathToSummaryHTML = new HashMap<>();

        JavascriptExecutor jse = ((JavascriptExecutor) driver);

        // adding data per modified source file
        List<WebElement> diffElems = driver.findElements(By.cssSelector("div[data-path]"));
        for (WebElement diffElem : diffElems) {
            String path = diffElem.getAttribute("data-path");

            // TODO: remove the following line
            //            if(!path.startsWith("src/"))
            //                path = path.substring(path.indexOf("/") + 1);

            Map<Integer, Integer> originalCoverage = originalCoverages.get(path),
                    patchedCoverage = patchedCoverages.get(path);

            if (!modifiedFiles.contains(path)) { // we only modify the diff for the modified files
                continue;
            }

            if (expand) {
                // clicking on needed expandable items
                while (true) {
                    List<WebElement> expandableElems = driver.findElements(By.cssSelector("a.js-expand"));
                    if (expandableElems == null || expandableElems.isEmpty()) break;

                    expandableElems.get(0).click();
                    Thread.sleep(SELENIUM_LOAD_WAIT_SEC);
                }

                // removing highlights for expanded lines
                jse.executeScript("Array.from(document.getElementsByClassName('blob-expanded'))"
                        + ".forEach(e => e.classList.remove('blob-expanded'))");
            }

            // Adding the exec info for the current file
            int currentLinesWithMoreExec = 0, currentLinesWithFewerExec = 0, currentLinesWithEqualExec = 0;
            boolean execHeaderAdded = false;
            List<WebElement> lineElems = ((WebElement) jse.executeScript("return arguments[0].parentNode;", diffElem))
                    .findElements(By.tagName("tr"));
            for (WebElement lineElem : lineElems) {
                List<WebElement> colElems = lineElem.findElements(By.tagName("td"));

                if (lineElem.getAttribute("class").contains("js-expandable-line")) { // its not a source line
                    jse.executeScript(
                            ("arguments[0].innerHTML = \"<td class=\\\"{classes}\\\" "
                                            + "style=\\\"text-align: center\\\">{exec-header}</td>\" + arguments[0].innerHTML")
                                    .replace("{classes}", colElems.get(0).getAttribute("class"))
                                    .replace("{exec-header}", !execHeaderAdded ? "EXEC-DIFF" : ""),
                            lineElem);
                    execHeaderAdded = true;
                    continue;
                }

                // extracting src and dst line numbers
                if (colElems.size() < 2) { // probably a comment in a PR
                    jse.executeScript(
                            ("arguments[0].innerHTML = \"<td class=\\\"{classes}\\\" "
                                            + "style=\\\"text-align: center\\\">{exec-header}</td>\" + arguments[0].innerHTML")
                                    .replace("{classes}", colElems.get(0).getAttribute("class"))
                                    .replace("{exec-header}", !execHeaderAdded ? "EXEC-DIFF" : ""),
                            lineElem);
                    execHeaderAdded = true;
                    continue;
                }
                String srcLineNumAttr = colElems.get(0).getAttribute("data-line-number"),
                        dstLineNumAttr = colElems.get(1).getAttribute("data-line-number");

                if ((srcLineNumAttr != null && !srcLineNumAttr.matches("-?\\d+"))
                        || (dstLineNumAttr != null && !dstLineNumAttr.matches("-?\\d+"))) {
                    jse.executeScript(
                            ("arguments[0].innerHTML = \"<td class=\\\"{classes}\\\" "
                                            + "style=\\\"text-align: center\\\">{exec-header}</td>\" + arguments[0].innerHTML")
                                    .replace("{classes}", colElems.get(0).getAttribute("class"))
                                    .replace("{exec-header}", !execHeaderAdded ? "EXEC-DIFF" : ""),
                            lineElem);
                    execHeaderAdded = true;
                    continue;
                }

                int srcLineNum = srcLineNumAttr == null ? -1 : Integer.parseInt(srcLineNumAttr),
                        dstLineNum = dstLineNumAttr == null ? -1 : Integer.parseInt(dstLineNumAttr);

                // computing exec-info
                int dstExecCnt = patchedCoverage.containsKey(dstLineNum) ? patchedCoverage.get(dstLineNum) : -1,
                        srcExecCnt = originalCoverage.containsKey(srcLineNum) ? originalCoverage.get(srcLineNum) : -1;

                String backgroundCol = "white";
                if (srcLineNum >= 0 && dstLineNum >= 0) {
                    int diffExecCnt =
                            !patchedCoverage.containsKey(dstLineNum) || !originalCoverage.containsKey(srcLineNum)
                                    ? 0
                                    : patchedCoverage.get(dstLineNum) - originalCoverage.get(srcLineNum);
                    currentLinesWithMoreExec += diffExecCnt > 0 ? 1 : 0;
                    currentLinesWithEqualExec += diffExecCnt == 0 ? 1 : 0;
                    currentLinesWithFewerExec += diffExecCnt < 0 ? 1 : 0;
                    backgroundCol = diffExecCnt > 0 ? "#ccffd8" : (diffExecCnt < 0 ? "#ffd7d5" : "white");
                }

                ExecInfo execInfo = getExecInfo(srcExecCnt, dstExecCnt);

                // adding exec-info
                jse.executeScript(
                        ("arguments[0].innerHTML = \"<td {title-info} style=\\\"background-color: {back-color}\\\" "
                                        + "no-empty-exec-info=\\\"{contains-exec-diff}\\\" "
                                        + "data-line-number=\\\"{exec-info-label}\\\" "
                                        + "class=\\\"{classes}\\\"></td>\" + arguments[0].innerHTML")
                                .replace("{exec-info-label}", execInfo.getLabel())
                                .replace("{title-info}", "title=\\\"" + execInfo.getTooltip() + "\\\"")
                                .replace("{classes}", colElems.get(1).getAttribute("class"))
                                .replace("{back-color}", backgroundCol)
                                .replace(
                                        "{contains-exec-diff}",
                                        ((currentLinesWithFewerExec + currentLinesWithFewerExec) != 0) + ""),
                        lineElem);
            }
            totalLinesWithMoreExec += currentLinesWithMoreExec;
            totalLinesWithFewerExec += currentLinesWithFewerExec;
            totalLinesWithEqualExec += currentLinesWithEqualExec;

            // adding summary info
            pathToSummaryHTML.put(
                    path,
                    addExecDiffSummary(
                            driver,
                            diffElem,
                            new GHReports.ReportSummary(
                                    currentLinesWithMoreExec,
                                    currentLinesWithFewerExec,
                                    currentLinesWithEqualExec,
                                    reportSummary == null ? null : reportSummary.getPathToSummaryHTML())));
        }

        return new GHReports.ReportSummary(
                totalLinesWithMoreExec, totalLinesWithFewerExec, totalLinesWithEqualExec, pathToSummaryHTML);
    }

    private static ExecInfo getExecInfo(int srcExecCnt, int dstExecCnt) {
        // creating the label
        String leftCol = "", rightCol = "";

        if (dstExecCnt < 0) {
            if (!(srcExecCnt < 0)) leftCol = toHumanReadableStr(srcExecCnt);
        } else {
            rightCol = toHumanReadableStr(dstExecCnt);

            if (!(srcExecCnt < 0)) {
                int diffExecCnt = dstExecCnt - srcExecCnt;
                leftCol = (diffExecCnt > 0 ? "+" : "") + toHumanReadableStr(diffExecCnt);
            }
        }

        // old version with two columns
        //        String label = leftCol + "&nbsp;".repeat(11 - leftCol.length() - rightCol.length()) + rightCol;

        // new version with one column and only for changed lines
        String labelContent = !leftCol.startsWith("+") && !leftCol.startsWith("-") ? "--" : leftCol,
                label =
                        "&nbsp;".repeat((11 - labelContent.length()) / 2)
                                + labelContent
                                + "&nbsp;".repeat(11 - labelContent.length() - (11 - labelContent.length()) / 2);

        // creating the tooltip
        String tooltip = "";
        if (dstExecCnt < 0) {
            if (!(srcExecCnt < 0)) tooltip = leftCol + " execution(s) in original version";
        } else {
            if (srcExecCnt < 0) tooltip = rightCol + " execution(s)  in the patched version";
            else
                tooltip = rightCol + " execution(s) in the patched, "
                        + ((srcExecCnt != dstExecCnt)
                                ? (leftCol + " compared to the original.")
                                : "no change compared to the original.");
        }
        return new ExecInfo(label, tooltip);
    }

    private static String toHumanReadableStr(int num) {
        if (Math.abs(num) >= Math.pow(10, 6)) return (int) (num / Math.pow(10, 6)) + "M";
        else if (Math.abs(num) >= Math.pow(10, 3)) return (int) (num / Math.pow(10, 3)) + "K";
        return num + "";
    }

    private static void addLinkToExpandedVersion(
            WebDriver nonExpandingDriver, WebDriver expandingDriver, String linkToExpanded, boolean showExpandWarning) {
        ((JavascriptExecutor) nonExpandingDriver)
                .executeScript(("var a = document.createElement('a');\n"
                                + "var linkText = document.createTextNode(\"See full diff\");\n"
                                + "a.appendChild(linkText);\n"
                                + "a.title = \"{title}\";\n"
                                + "a.href = \"{link-to-full}\";\n"
                                + "a.className = \"float-right ml-2\";\n"
                                + "var item = document.querySelector(\".d-flex.d-inline-block.float-right\") == null ? document.querySelector(\"#diffstat\") "
                                + ": document.querySelector(\".d-flex.d-inline-block.float-right\");"
                                + "item.parentNode.replaceChild(a, item);")
                        .replace("{link-to-full}", linkToExpanded)
                        .replace(
                                "{title}",
                                "There are " + (showExpandWarning ? "some" : "no")
                                        + " execution trace diffs not visible on this page."));

        ((JavascriptExecutor) expandingDriver)
                .executeScript(
                        "if (document.querySelector(\".d-flex.d-inline-block.float-right\") == null) document.querySelector(\"#diffstat\").remove(); "
                                + "else document.querySelector(\".d-flex.d-inline-block.float-right\").remove();");
    }

    private static boolean expandedContainsExecDiff(WebDriver nonExpandingDriver, WebDriver expandingDriver) {
        return nonExpandingDriver
                        .findElements(By.cssSelector("td[no-empty-exec-info='true']"))
                        .size()
                != expandingDriver
                        .findElements(By.cssSelector("td[no-empty-exec-info='true']"))
                        .size();
    }

    private static class ExecInfo {
        private String label, tooltip;

        public ExecInfo(String label, String tooltip) {
            this.label = label;
            this.tooltip = tooltip;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getTooltip() {
            return tooltip;
        }

        public void setTooltip(String tooltip) {
            this.tooltip = tooltip;
        }
    }

    public static File getGHDiff(String slug, String commit, SourceInfo srcInfo, SourceInfo dstInfo)
            throws IOException, InterruptedException {
        File ghDiff = saveGhDiffInTempFile(getCommitUrl(slug, commit));

        Document doc = Jsoup.parse(ghDiff, "UTF-8");

        doc.outputSettings().prettyPrint(false);

        Elements srcRows = doc.selectFirst("tbody").children();

        CodeInterval srcMethodCxtInterval = new CodeInterval(), dstMethodCxtInterval = new CodeInterval();

        for (Element tr : srcRows) {
            Elements cols = tr.children();
            try {
                if (cols.get(0).hasAttr("data-line-number") && !cols.get(1).hasAttr("data-line-number")) {
                    srcMethodCxtInterval = srcInfo.getContainingMethodContextInterval(
                            Integer.parseInt(cols.get(0).attr("data-line-number")));
                }
                if (!cols.get(0).hasAttr("data-line-number") && cols.get(1).hasAttr("data-line-number")) {
                    dstMethodCxtInterval = dstInfo.getContainingMethodContextInterval(
                            Integer.parseInt(cols.get(1).attr("data-line-number")));
                }
            } catch (NumberFormatException e) {
            }
        }

        for (Element tr : srcRows) {
            Elements cols = tr.children();
            boolean outOfMethod = true;
            try {
                if (cols.get(0).hasAttr("data-line-number")) {
                    int line = Integer.parseInt(cols.get(0).attr("data-line-number"));
                    outOfMethod = outOfMethod && !srcMethodCxtInterval.covers(line);
                }
                if (cols.get(1).hasAttr("data-line-number")) {
                    int line = Integer.parseInt(cols.get(1).attr("data-line-number"));
                    outOfMethod = outOfMethod && !dstMethodCxtInterval.covers(line);
                }
            } catch (NumberFormatException e) {
            }

            if (outOfMethod) tr.remove();
        }

        FileUtils.writeStringToFile(ghDiff, doc.outerHtml(), "UTF-8");

        return ghDiff;
    }

    public static enum ChangeType {
        PR,
        COMMIT
    }
}
