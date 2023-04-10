package se.assertkth.tracediff.models;

public class CodeInterval {
    private Integer startLine, endLine;

    public CodeInterval () {
        this.startLine = 1000000000;
        this.endLine = -1;
    }

    public boolean covers(int line){
        return isValid() && line >= startLine && line < endLine;
    }

    public void expandIfNeeded(int startLine, int endLine){
        this.startLine = Math.min(this.startLine, startLine);
        this.endLine = Math.max(this.endLine, endLine);
    }

    public boolean isValid(){
        return endLine > -1;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }
}
