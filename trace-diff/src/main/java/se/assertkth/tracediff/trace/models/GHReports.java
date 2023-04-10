package se.assertkth.tracediff.trace.models;

import java.util.Map;

public class GHReports {
    private String expandedReport, unexpandedReport;
    private ReportSummary summary;

    public GHReports(String expandedReport, String unexpandedReport, ReportSummary summary) {
        this.expandedReport = expandedReport;
        this.unexpandedReport = unexpandedReport;
        this.summary = summary;
    }

    public String getExpandedReport() {
        return expandedReport;
    }

    public void setExpandedReport(String expandedReport) {
        this.expandedReport = expandedReport;
    }

    public String getUnexpandedReport() {
        return unexpandedReport;
    }

    public void setUnexpandedReport(String unexpandedReport) {
        this.unexpandedReport = unexpandedReport;
    }

    public ReportSummary getSummary() {
        return summary;
    }

    public void setSummary(ReportSummary summary) {
        this.summary = summary;
    }

    public static class ReportSummary {
        private int linesWithMoreExec, linesWithFewerExec, linesWithEqualExec;
        private Map<String, String> pathToSummaryHTML;

        public ReportSummary(
                int linesWithMoreExec,
                int linesWithFewerExec,
                int linesWithEqualExec,
                Map<String, String> pathToSummaryHTML) {
            this.linesWithMoreExec = linesWithMoreExec;
            this.linesWithFewerExec = linesWithFewerExec;
            this.linesWithEqualExec = linesWithEqualExec;
            this.pathToSummaryHTML = pathToSummaryHTML;
        }

        public int getLinesWithEqualExec() {
            return linesWithEqualExec;
        }

        public void setLinesWithEqualExec(int linesWithEqualExec) {
            this.linesWithEqualExec = linesWithEqualExec;
        }

        public int getLinesWithFewerExec() {
            return linesWithFewerExec;
        }

        public void setLinesWithFewerExec(int linesWithFewerExec) {
            this.linesWithFewerExec = linesWithFewerExec;
        }

        public int getLinesWithMoreExec() {
            return linesWithMoreExec;
        }

        public void setLinesWithMoreExec(int linesWithMoreExec) {
            this.linesWithMoreExec = linesWithMoreExec;
        }

        public Map<String, String> getPathToSummaryHTML() {
            return pathToSummaryHTML;
        }

        public void setPathToSummaryHTML(Map<String, String> pathToSummaryHTML) {
            this.pathToSummaryHTML = pathToSummaryHTML;
        }
    }
}
