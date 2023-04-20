package io.github.chains_project.tracediff.trace.models;

import java.util.Map;
import java.util.stream.Collectors;

public class TraceInfo {
    private Map<String, LineMapping> pathToLineMapping;
    private Map<String, Map<Integer, Integer>> pathToOriginalCoverage, pathToPatchedCoverage;

    public TraceInfo(
            Map<String, LineMapping> pathToLineMapping,
            Map<String, Map<Integer, Integer>> pathToOriginalCoverage,
            Map<String, Map<Integer, Integer>> pathToPatchedCoverage) {
        this.pathToLineMapping = pathToLineMapping;
        this.pathToOriginalCoverage = pathToOriginalCoverage;
        this.pathToPatchedCoverage = pathToPatchedCoverage;
    }

    // returns map from file to a map of patched line number and execution time diff
    public Map<String, Map<Integer, Integer>> getCoverageDiffs() {
        return pathToPatchedCoverage.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, fileCoverage -> {
            String path = fileCoverage.getKey();
            return fileCoverage.getValue().entrySet().stream()
                    .filter(lineCoverage ->
                            pathToLineMapping.get(path).getDstToSrc().containsKey(lineCoverage.getKey())
                                    && pathToOriginalCoverage
                                                    .get(path)
                                                    .get(pathToLineMapping
                                                            .get(path)
                                                            .getDstToSrc()
                                                            .get(lineCoverage.getKey()))
                                            != lineCoverage.getValue())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            lineCoverage -> lineCoverage.getValue()
                                    - pathToOriginalCoverage
                                            .get(path)
                                            .get(pathToLineMapping
                                                    .get(path)
                                                    .getDstToSrc()
                                                    .get(lineCoverage.getKey()))));
        }));
    }

    public Map<String, LineMapping> getPathToLineMapping() {
        return pathToLineMapping;
    }

    public void setPathToLineMapping(Map<String, LineMapping> pathToLineMapping) {
        this.pathToLineMapping = pathToLineMapping;
    }

    public Map<String, Map<Integer, Integer>> getPathToOriginalCoverage() {
        return pathToOriginalCoverage;
    }

    public void setPathToOriginalCoverage(Map<String, Map<Integer, Integer>> pathToOriginalCoverage) {
        this.pathToOriginalCoverage = pathToOriginalCoverage;
    }

    public Map<String, Map<Integer, Integer>> getPathToPatchedCoverage() {
        return pathToPatchedCoverage;
    }

    public void setPathToPatchedCoverage(Map<String, Map<Integer, Integer>> pathToPatchedCoverage) {
        this.pathToPatchedCoverage = pathToPatchedCoverage;
    }
}
