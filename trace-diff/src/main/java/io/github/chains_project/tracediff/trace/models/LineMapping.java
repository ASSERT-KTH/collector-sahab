package io.github.chains_project.tracediff.trace.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LineMapping {
    private Map<Integer, Integer> srcToDst, dstToSrc;
    private Set<Integer> srcNewLines, dstNewLines;

    public LineMapping() {
        srcToDst = new HashMap<>();
        dstToSrc = new HashMap<>();
        srcNewLines = new HashSet<>();
        dstNewLines = new HashSet<>();
    }

    public void addMapping(int src, int dst) {
        srcToDst.put(src, dst);
        dstToSrc.put(dst, src);
    }

    public Map<Integer, Integer> getSrcToDst() {
        return srcToDst;
    }

    public void setSrcToDst(Map<Integer, Integer> srcToDst) {
        this.srcToDst = srcToDst;
    }

    public Map<Integer, Integer> getDstToSrc() {
        return dstToSrc;
    }

    public void setDstToSrc(Map<Integer, Integer> dstToSrc) {
        this.dstToSrc = dstToSrc;
    }

    public Set<Integer> getDstNewLines() {
        return dstNewLines;
    }

    public void setDstNewLines(Set<Integer> dstNewLines) {
        this.dstNewLines = dstNewLines;
    }

    public Set<Integer> getSrcNewLines() {
        return srcNewLines;
    }

    public void setSrcNewLines(Set<Integer> srcNewLines) {
        this.srcNewLines = srcNewLines;
    }
}
