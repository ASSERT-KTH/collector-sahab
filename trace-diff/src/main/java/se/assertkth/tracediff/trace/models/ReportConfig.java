package se.assertkth.tracediff.trace.models;

public class ReportConfig {
    private boolean showHits, allColors;

    public  ReportConfig(){
        this.showHits = true;
        this.allColors = true;
    }

    public ReportConfig(String configStr){
        // TODO: clean it!
        this.showHits = configStr.contains("showHits");
        this.allColors = configStr.contains("allColors");
    }

    public boolean isShowHits() {
        return showHits;
    }

    public void setShowHits(boolean showHits) {
        this.showHits = showHits;
    }

    public boolean isAllColors() {
        return allColors;
    }

    public void setAllColors(boolean allColors) {
        this.allColors = allColors;
    }

    @Override
    public String toString() {
        return showHits + "-" + allColors;
    }
}
