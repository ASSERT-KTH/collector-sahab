package io.github.chains_project.tracediff.statediff.computer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chains_project.cs.commons.Pair;
import io.github.chains_project.cs.commons.runtime.LineSnapshot;
import io.github.chains_project.cs.commons.runtime.RuntimeReturnedValue;
import io.github.chains_project.cs.commons.runtime.RuntimeValue;
import io.github.chains_project.cs.commons.runtime.output.SahabOutput;
import io.github.chains_project.tracediff.Constants;
import io.github.chains_project.tracediff.statediff.models.ProgramStateDiff;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class StateDiffComputer {
    private File leftSahabReportDir, rightSahabReportDir;
    private Map<Integer, Integer> leftRightLineMapping, rightLeftLineMapping;
    private Map<Integer, Set<String>> leftLineToVars, rightLineToVars;
    private List<String> tests;

    public StateDiffComputer(
            File leftSahabReportDir,
            File rightSahabReportDir,
            Map<Integer, Integer> leftRightLineMapping,
            Map<Integer, Integer> rightLeftLineMapping,
            Map<Integer, Set<String>> leftLineToVars,
            Map<Integer, Set<String>> rightLineToVars,
            List<String> tests)
            throws IOException {
        this.leftSahabReportDir = leftSahabReportDir;
        this.rightSahabReportDir = rightSahabReportDir;
        this.leftLineToVars = leftLineToVars;
        this.rightLineToVars = rightLineToVars;
        this.leftRightLineMapping = leftRightLineMapping;
        this.rightLeftLineMapping = rightLeftLineMapping;
        this.tests = tests;
    }

    public ProgramStateDiff computeProgramStateDiff() throws IOException {
        ProgramStateDiff programStateDiff = new ProgramStateDiff();

        programStateDiff.setFirstOriginalUniqueStateSummary(getFirstDistinctStateOnRelevantLine(
                leftLineToVars, leftRightLineMapping, leftSahabReportDir, rightSahabReportDir));

        programStateDiff.setFirstPatchedUniqueStateSummary(getFirstDistinctStateOnRelevantLine(
                rightLineToVars, rightLeftLineMapping, rightSahabReportDir, leftSahabReportDir));

        programStateDiff.setOriginalUniqueReturn(
                getFirstUniqueReturn(leftRightLineMapping, leftSahabReportDir, rightSahabReportDir));

        programStateDiff.setPatchedUniqueReturn(
                getFirstUniqueReturn(rightLeftLineMapping, rightSahabReportDir, leftSahabReportDir));

        return programStateDiff;
    }

    private ProgramStateDiff.UniqueReturnSummary getFirstUniqueReturn(
            Map<Integer, Integer> lineMapping, File sahabReportDir, File oppositeSahabReportDir) throws IOException {

        ProgramStateDiff.UniqueReturnSummary firstUniqueReturnSummary = new ProgramStateDiff.UniqueReturnSummary();
        ObjectMapper mapper = new ObjectMapper();

        Map<Integer, Integer> reverseLineMapping =
                lineMapping.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        Set<Integer> oppositeHashes = new HashSet<>();

        int cnt = 0;
        Path reportPath = oppositeSahabReportDir.toPath().resolve(cnt + ".json");
        while (reportPath.toFile().exists()) {
            SahabOutput sahabOutputRight = mapper.readValue(
                    new FileReader(reportPath.toFile(), StandardCharsets.UTF_8), new TypeReference<>() {});
            List<RuntimeReturnedValue> jsonStates = sahabOutputRight.getReturns();
            oppositeHashes.addAll(getHashedReturnStates(jsonStates, reverseLineMapping, true));

            reportPath = oppositeSahabReportDir.toPath().resolve(++cnt + ".json");
        }

        reportPath = sahabReportDir.toPath().resolve("0.json");
        SahabOutput sahabOutputLeft =
                mapper.readValue(new FileReader(reportPath.toFile(), StandardCharsets.UTF_8), new TypeReference<>() {});
        List<RuntimeReturnedValue> jsonStates = sahabOutputLeft.getReturns();

        Set<Integer> hashes = getHashedReturnStates(jsonStates, reverseLineMapping, false);
        hashes.removeAll(oppositeHashes);

        List<Pair<Integer, String>> lastVarVals = null;
        if (cnt == 1) {
            lastVarVals = getReturnStateVals(jsonStates);
        }

        for (int i = 1; i < cnt; i++) {
            reportPath = sahabReportDir.toPath().resolve(i + ".json");
            sahabOutputLeft = mapper.readValue(
                    new FileReader(reportPath.toFile(), StandardCharsets.UTF_8), new TypeReference<>() {});
            jsonStates = sahabOutputLeft.getReturns();
            hashes.retainAll(getHashedReturnStates(jsonStates, reverseLineMapping, false));

            if (i == cnt - 1) {
                lastVarVals = getReturnStateVals(jsonStates);
            }
        }

        for (int i = 0; i < lastVarVals.size(); i++) {
            Pair<Integer, String> p = lastVarVals.get(i);
            if (hashes.contains(getVarValHash(p.getLeft(), p.getRight()))) {
                // FIXME: this is not correct, we should get the test that executed the line
                String executedTest = getMatchingTest(jsonStates.get(0).getStackTrace());
                firstUniqueReturnSummary.setDifferencingTest(executedTest);
                firstUniqueReturnSummary.setFirstUniqueVarValLine(p.getLeft());
                firstUniqueReturnSummary.setFirstUniqueVarVal(p.getRight());

                RuntimeReturnedValue outerMostReturn = jsonStates.stream()
                        .filter(rt -> Integer.parseInt(rt.getLocation().split(":")[1]) == p.getLeft())
                        .findFirst()
                        .get();
                firstUniqueReturnSummary.setFirstUniqueVarValType(
                        findType("{return-object}", p.getRight(), List.of(outerMostReturn)));
                break;
            }
        }

        return firstUniqueReturnSummary;
    }

    private List<Pair<Integer, String>> getReturnStateVals(List<RuntimeReturnedValue> jsonStates) {
        List<Pair<Integer, String>> ret = new ArrayList<>();

        for (int i = 0; i < jsonStates.size(); i++) {
            RuntimeReturnedValue jo = jsonStates.get(i);
            int linenumber = Integer.parseInt(jo.getLocation().split(":")[1]);
            List<String> returnVarVals = new ArrayList<>(extractVarVals("{return-object}", jo));
            Collections.sort(returnVarVals, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    if (StringUtils.countMatches(s1, '.') < StringUtils.countMatches(s2, '.')) return -1;
                    if (StringUtils.countMatches(s1, '.') > StringUtils.countMatches(s2, '.')) return 1;
                    if (s1.length() < s2.length()) return -1;
                    if (s1.length() > s2.length()) return 1;
                    return 0;
                }
            });
            returnVarVals.forEach(varVal -> ret.add(new Pair<Integer, String>(linenumber, varVal)));
        }

        return ret;
    }

    private Set<Integer> getHashedReturnStates(
            List<RuntimeReturnedValue> ja, Map<Integer, Integer> reverseLineMapping, boolean isOpposite) {
        Set<Integer> hashes = new HashSet<>();

        for (int i = 0; i < ja.size(); i++) {
            RuntimeReturnedValue jo = ja.get(i);
            hashes.addAll(returnStateJsonToHash(jo, reverseLineMapping, isOpposite));
        }

        return hashes;
    }

    // return first state in @hashes that does not exist in states of the corresponding opposite line
    private ProgramStateDiff.UniqueStateSummary getFirstDistinctStateOnRelevantLine(
            Map<Integer, Set<String>> lineToVars,
            Map<Integer, Integer> lineMapping,
            File sahabReportDir,
            File oppositeSahabReportDir)
            throws IOException {
        ProgramStateDiff.UniqueStateSummary firstUniqueStateSummary = new ProgramStateDiff.UniqueStateSummary();
        ObjectMapper mapper = new ObjectMapper();

        Map<Integer, Integer> reverseLineMapping =
                lineMapping.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        Set<Integer> oppositeHashes = new HashSet<>();

        int cnt = 0;
        Path reportPath = oppositeSahabReportDir.toPath().resolve(cnt + ".json");
        while (reportPath.toFile().exists()) {
            SahabOutput sahabOutputRight = mapper.readValue(
                    new FileReader(reportPath.toFile(), StandardCharsets.UTF_8), new TypeReference<>() {});
            List<LineSnapshot> oppositeLineSnapshots = sahabOutputRight.getBreakpoint();
            oppositeHashes.addAll(getVarValHashes(oppositeLineSnapshots, lineToVars, reverseLineMapping, true));

            reportPath = oppositeSahabReportDir.toPath().resolve(++cnt + ".json");
        }

        reportPath = sahabReportDir.toPath().resolve("0.json");
        SahabOutput sahabOutputLeft =
                mapper.readValue(new FileReader(reportPath.toFile(), StandardCharsets.UTF_8), new TypeReference<>() {});
        List<LineSnapshot> lineSnapshots = sahabOutputLeft.getBreakpoint();

        Set<Integer> hashes = getVarValHashes(lineSnapshots, lineToVars, reverseLineMapping, false);
        hashes.removeAll(oppositeHashes);

        List<Pair<Integer, String>> lastVarVals = null;
        if (cnt == 1) {
            lastVarVals = getLineStateVals(lineSnapshots, lineToVars);
        }

        for (int i = 1; i < cnt; i++) {
            reportPath = sahabReportDir.toPath().resolve(i + ".json");
            sahabOutputLeft = mapper.readValue(
                    new FileReader(reportPath.toFile(), StandardCharsets.UTF_8), new TypeReference<>() {});
            lineSnapshots = sahabOutputLeft.getBreakpoint();
            hashes.retainAll(getVarValHashes(lineSnapshots, lineToVars, reverseLineMapping, false));

            if (i == cnt - 1) {
                lastVarVals = getLineStateVals(lineSnapshots, lineToVars);
            }
        }

        for (int i = 0; i < lastVarVals.size(); i++) {
            Pair<Integer, String> p = lastVarVals.get(i);
            if (hashes.contains(getVarValHash(p.getLeft(), p.getRight()))) {
                // FIXME: this is not correct, we should get the test that executed the line
                String executedTest = getMatchingTest(
                        lineSnapshots.get(0).getStackFrameContext().get(0).getStackTrace());
                firstUniqueStateSummary.setDifferencingTest(executedTest);
                firstUniqueStateSummary.setFirstUniqueVarValLine(p.getLeft());
                firstUniqueStateSummary.setFirstUniqueVarVal(p.getRight());

                LineSnapshot outerMostLine = lineSnapshots.stream()
                        .filter(lineSnapshot -> lineSnapshot.getLineNumber() == p.getLeft())
                        .findFirst()
                        .get();
                List<RuntimeValue> candidateRuntimeValues =
                        outerMostLine.getStackFrameContext().get(0).getRuntimeValueCollection();

                firstUniqueStateSummary.setFirstUniqueVarValType(findType("", p.getRight(), candidateRuntimeValues));
                break;
            }
        }

        return firstUniqueStateSummary;
    }

    private String findType(String prefix, String expectedHash, List<RuntimeValue> candidateRuntimeValues) {
        for (RuntimeValue runtimeValue : candidateRuntimeValues) {

            RuntimeValue runtimeValueFromHash = getRuntimeValueFromHash(prefix, runtimeValue, expectedHash);

            if (runtimeValueFromHash != null) {
                return runtimeValueFromHash.getType();
            }
        }
        return null;
    }

    private RuntimeValue getRuntimeValueFromHash(String prefix, RuntimeValue valueJo, String expectedHash) {
        if (Constants.FILE_RELATED_CLASSES.stream().anyMatch(valueJo.getType()::contains)) {
            return valueJo;
        }

        if (valueJo.getFields() != null && !valueJo.getFields().isEmpty()) {
            if (valueJo.getKind() == RuntimeValue.Kind.RETURN) {
                prefix += ".";
            } else {
                prefix += (valueJo.getName() == null ? "" : valueJo.getName() + ".");
            }
            List<RuntimeValue> nestedTypes = valueJo.getFields();

            for (RuntimeValue nestedObj : nestedTypes) {
                RuntimeValue runtimeValue = getRuntimeValueFromHash(prefix, nestedObj, expectedHash);
                if (runtimeValue != null) {
                    return runtimeValue;
                }
            }
        } else if (valueJo.getArrayElements() != null
                && !valueJo.getArrayElements().isEmpty()) {
            List<RuntimeValue> nestedTypes = valueJo.getArrayElements();

            prefix += (valueJo.getName() == null ? "" : valueJo.getName());

            for (int i = 0; i < nestedTypes.size(); i++) {
                RuntimeValue nestedObj = nestedTypes.get(i);
                String currentPrefix = prefix + "[" + i + "].";
                RuntimeValue runtimeValue = getRuntimeValueFromHash(currentPrefix, nestedObj, expectedHash);
                if (runtimeValue != null) {
                    return runtimeValue;
                }
            }
        } else {
            // it's a leaf node
            String currentPrefix;
            if (valueJo.getKind() == RuntimeValue.Kind.RETURN) {
                currentPrefix = prefix;
            } else {
                currentPrefix = prefix + (valueJo.getName() == null ? "" : valueJo.getName());
            }
            String value = String.valueOf(valueJo.getValue());
            String actualHash = currentPrefix + "=" + value;
            if (actualHash.equals(expectedHash)) {
                return valueJo;
            }
        }
        return null;
    }

    private int getVarValHash(int line, String varVal) {
        return (line + ":" + varVal).hashCode();
    }

    private Set<Integer> getVarValHashes(
            List<LineSnapshot> lineSnapshots,
            Map<Integer, Set<String>> lineToVars,
            Map<Integer, Integer> reverseLineMapping,
            boolean isOpposite) {
        Set<Integer> varValHashes = new HashSet<>();

        for (int i = 0; i < lineSnapshots.size(); i++) {
            LineSnapshot lineSnapshot = lineSnapshots.get(i);

            if (isOpposite) {
                if (!reverseLineMapping.containsKey(lineSnapshot.getLineNumber())) continue;
            } else {
                if (!reverseLineMapping.containsValue(lineSnapshot.getLineNumber())) continue;
            }

            int originalLineNumber =
                    !isOpposite ? lineSnapshot.getLineNumber() : reverseLineMapping.get(lineSnapshot.getLineNumber());

            if (!lineToVars.containsKey(originalLineNumber)) continue;

            extractVarVals(
                            lineSnapshot.getStackFrameContext().get(0).getRuntimeValueCollection(),
                            lineToVars.get(originalLineNumber),
                            true)
                    .forEach(s -> varValHashes.add(getVarValHash(originalLineNumber, s)));
        }

        return varValHashes;
    }

    private List<Pair<Integer, String>> getLineStateVals(
            List<LineSnapshot> lineSnapshots, Map<Integer, Set<String>> lineToVars) {
        List<Pair<Integer, String>> lineStateVals = new ArrayList<>();

        for (int i = 0; i < lineSnapshots.size(); i++) {
            LineSnapshot lineSnapshot = lineSnapshots.get(i);

            if (!lineToVars.containsKey(lineSnapshot.getLineNumber())) continue;

            List<String> varVals = new ArrayList<>(extractVarVals(
                    lineSnapshot.getStackFrameContext().get(0).getRuntimeValueCollection(),
                    lineToVars.get(lineSnapshot.getLineNumber()),
                    true));
            Collections.sort(varVals, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    if (StringUtils.countMatches(s1, '.') < StringUtils.countMatches(s2, '.')) return -1;
                    if (StringUtils.countMatches(s1, '.') > StringUtils.countMatches(s2, '.')) return 1;
                    if (s1.length() < s2.length()) return -1;
                    if (s1.length() > s2.length()) return 1;
                    return 0;
                }
            });

            varVals.forEach(s -> lineStateVals.add(new Pair(lineSnapshot.getLineNumber(), s)));
        }

        return lineStateVals;
    }

    private String getMatchingTest(List<String> stackTraceJa) {
        if (tests.size() == 1) return tests.get(0);

        for (String test : tests) {
            String[] testParts = test.split(Constants.TEST_METHOD_NAME_SEPARATOR);
            String testClass = testParts[0], testMethod = testParts.length > 1 ? testParts[1] : null;

            for (String stackItem : stackTraceJa) {
                if (stackItem.contains(testClass) && (testMethod != null && stackItem.contains(testMethod)))
                    return test;
            }
        }
        return Constants.UNKNOWN_TEST;
    }

    private Set<String> extractVarVals(
            List<RuntimeValue> valueCollection, Set<String> lineVarsLst, boolean checkLineVars) {
        Set<String> varVals = new HashSet<>();

        for (RuntimeValue valueJo : valueCollection) {
            // a variable that is not important in this line should be ignored
            if (checkLineVars && (lineVarsLst == null || !lineVarsLst.contains(valueJo.getName()))) continue;

            varVals.addAll(extractVarVals("", valueJo));
        }
        return varVals;
    }

    private Set<String> extractVarVals(String prefix, RuntimeValue valueJo) {
        Set<String> varVals = new HashSet<>();

        if (Constants.FILE_RELATED_CLASSES.stream().anyMatch(valueJo.getType()::contains)) {
            return varVals;
        }

        if (valueJo.getFields() != null && !valueJo.getFields().isEmpty()) {
            if (valueJo.getKind() == RuntimeValue.Kind.RETURN) {
                prefix += ".";
            } else {
                prefix += (valueJo.getName() == null ? "" : valueJo.getName() + ".");
            }
            List<RuntimeValue> nestedTypes = valueJo.getFields();

            for (RuntimeValue nestedObj : nestedTypes) {
                varVals.addAll(extractVarVals(prefix, nestedObj));
            }
        } else if (valueJo.getArrayElements() != null
                && !valueJo.getArrayElements().isEmpty()) {
            List<RuntimeValue> nestedTypes = valueJo.getArrayElements();

            prefix += (valueJo.getName() == null ? "" : valueJo.getName());

            for (int i = 0; i < nestedTypes.size(); i++) {
                RuntimeValue nestedObj = nestedTypes.get(i);
                String currentPrefix = prefix + "[" + i + "].";
                varVals.addAll(extractVarVals(currentPrefix, nestedObj));
            }
        } else {
            // it's a leaf node
            String currentPrefix;
            if (valueJo.getKind() == RuntimeValue.Kind.RETURN) {
                currentPrefix = prefix;
            } else {
                currentPrefix = prefix + (valueJo.getName() == null ? "" : valueJo.getName());
            }
            String value = String.valueOf(valueJo.getValue());
            varVals.add(currentPrefix + "=" + value);
        }

        return varVals;
    }

    private Set<Integer> returnStateJsonToHash(
            RuntimeReturnedValue stateJO, Map<Integer, Integer> reverseLineMapping, boolean isOpposite) {
        int lineNumber = Integer.parseInt(stateJO.getLocation().split(":")[1]);
        if (isOpposite) {
            if (!reverseLineMapping.containsKey(lineNumber)) return new HashSet<>();
        } else {
            if (!reverseLineMapping.containsValue(lineNumber)) return new HashSet<>();
        }

        final int finalLineNumber = !isOpposite ? lineNumber : reverseLineMapping.get(lineNumber);
        return extractVarVals("{return-object}", stateJO).stream()
                .map(s -> getVarValHash(finalLineNumber, s))
                .collect(Collectors.toSet());
    }
}
