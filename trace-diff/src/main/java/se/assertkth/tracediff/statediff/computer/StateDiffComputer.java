package se.assertkth.tracediff.statediff.computer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import se.assertkth.cs.commons.Pair;
import se.assertkth.cs.commons.runtime.LineSnapshot;
import se.assertkth.cs.commons.runtime.RuntimeReturnedValue;
import se.assertkth.cs.commons.runtime.RuntimeValue;
import se.assertkth.cs.commons.runtime.StackFrameContext;
import se.assertkth.cs.commons.runtime.output.SahabOutput;
import se.assertkth.tracediff.Constants;
import se.assertkth.tracediff.models.VarValsSet;
import se.assertkth.tracediff.statediff.models.ProgramStateDiff;

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

    public ProgramStateDiff computeProgramStateDiff(PrintWriter diffPrinter, boolean excludeRandomValues) throws IOException {
        ProgramStateDiff programStateDiff = new ProgramStateDiff();

        programStateDiff.setFirstOriginalUniqueStateSummary(getFirstDistinctStateOnRelevantLine(
                leftLineToVars, leftRightLineMapping, leftSahabReportDir, rightSahabReportDir, diffPrinter, excludeRandomValues));

        programStateDiff.setFirstPatchedUniqueStateSummary(getFirstDistinctStateOnRelevantLine(
                rightLineToVars, rightLeftLineMapping, rightSahabReportDir, leftSahabReportDir, diffPrinter, excludeRandomValues));

        programStateDiff.setOriginalUniqueReturn(
                getFirstUniqueReturn(leftRightLineMapping, leftSahabReportDir, rightSahabReportDir, diffPrinter, excludeRandomValues));

        programStateDiff.setPatchedUniqueReturn(
                getFirstUniqueReturn(rightLeftLineMapping, rightSahabReportDir, leftSahabReportDir, diffPrinter, excludeRandomValues));

        return programStateDiff;
    }

    private void printIfNeeded(String title, String line, PrintWriter printer) {
        if (printer != null) {
            printer.println(title);
            printer.println(line);
            printer.flush();
        }
    }

    private ProgramStateDiff.UniqueReturnSummary getFirstUniqueReturn(
            Map<Integer, Integer> lineMapping,
            File sahabReportDir,
            File oppositeSahabReportDir,
            PrintWriter diffPrinter,
            boolean excludeRandomValues)
            throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        SahabOutput sahabOutputLeft =
                mapper.readValue(new FileReader(sahabReportDir.toPath().resolve("0.json").toFile(),
                        StandardCharsets.UTF_8), SahabOutput.class);
        List<RuntimeReturnedValue> jsonStates = sahabOutputLeft.getReturns();

        SahabOutput sahabOutputRight =
                mapper.readValue(new FileReader(oppositeSahabReportDir.toPath().resolve("0.json").toFile(),
                        StandardCharsets.UTF_8), SahabOutput.class);
        List<RuntimeReturnedValue> oppositeJsonStates = sahabOutputRight.getReturns();

        List<Pair<Integer, Integer>> hashes = getHashedReturnStates(jsonStates),
                oppositeHashes = getHashedReturnStates(oppositeJsonStates);

        Map<Integer, List<Integer>> oppositeLineToStateIndices = getLineToStateIndices(oppositeHashes);

        Map<Integer, Set<Integer>> oppositeLineToStates = getLineToStates(oppositeHashes);

        ProgramStateDiff.UniqueReturnSummary firstUniqueReturnSummary = new ProgramStateDiff.UniqueReturnSummary();

        for (int i = 0; i < hashes.size(); i++) {
            Pair<Integer, Integer> p = hashes.get(i);
            int lineNumber = p.getLeft(), stateHash = p.getRight();

            if (!lineMapping.containsKey(lineNumber)) continue;

            int oppositeLineNumber = lineMapping.get(lineNumber);

            if (!oppositeLineToStates.containsKey(oppositeLineNumber)
                    || !oppositeLineToStates.get(oppositeLineNumber).contains(stateHash)) {
                // this stateHash is not covered in the opposite version

                printIfNeeded(
                        "Unique return state at line " + lineNumber,
                        jsonStates.get(i).toString(),
                        diffPrinter);

                if (firstUniqueReturnSummary.getFirstUniqueReturnHash() == null) {
                    firstUniqueReturnSummary.setFirstUniqueReturnHash(stateHash);
                    firstUniqueReturnSummary.setFirstUniqueReturnLine(lineNumber);
                }

                VarValsSet distinctVarVals = identifyDistinctReturnVarVal(
                        jsonStates.get(i), oppositeJsonStates, oppositeLineToStateIndices.get(oppositeLineNumber));

                for (String varVal : distinctVarVals.getAllVals()) {
                    printIfNeeded("Unique return var-val at line " + lineNumber, varVal, diffPrinter);
                }

                String selectedVarVal = distinctVarVals.getSelectedVal();

                if (selectedVarVal != null && firstUniqueReturnSummary.getFirstUniqueVarVal() == null) {
                    String executedTest = getMatchingTest(jsonStates.get(i).getStackTrace());
                    firstUniqueReturnSummary.setDifferencingTest(executedTest);
                    firstUniqueReturnSummary.setFirstUniqueVarValLine(lineNumber);
                    firstUniqueReturnSummary.setFirstUniqueVarVal(selectedVarVal);

                    if (diffPrinter == null) break;
                }
            }
        }

        return firstUniqueReturnSummary;
    }

    private List<Pair<Integer, Integer>> getHashedReturnStates(List<RuntimeReturnedValue> ja) {
        List<Pair<Integer, Integer>> hashes = new ArrayList<>();

        for (int i = 0; i < ja.size(); i++) {
            RuntimeReturnedValue jo = ja.get(i);
            hashes.add(returnStateJsonToHash(jo));
        }

        return hashes;
    }

    // return first state in @hashes that does not exist in states of the corresponding opposite line
    private ProgramStateDiff.UniqueStateSummary getFirstDistinctStateOnRelevantLine(
            Map<Integer, Set<String>> lineToVars,
            Map<Integer, Integer> lineMapping,
            File sahabReportDir,
            File oppositeSahabReportDir,
            PrintWriter diffPrinter,
            boolean excludeRandomValues)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        SahabOutput sahabOutputLeft =
                mapper.readValue(new FileReader(sahabReportDir, StandardCharsets.UTF_8), new TypeReference<>() {});
        List<LineSnapshot> jsonStates = sahabOutputLeft.getBreakpoint();

        SahabOutput sahabOutputRight = mapper.readValue(
                new FileReader(oppositeSahabReportDir, StandardCharsets.UTF_8), new TypeReference<>() {});
        List<LineSnapshot> oppositeJsonStates = sahabOutputRight.getBreakpoint();

        List<Pair<Integer, Integer>> hashes = getHashedBreakpointStates(jsonStates),
                oppositeHashes = getHashedBreakpointStates(oppositeJsonStates);

        Map<Integer, List<Integer>> oppositeLineToStateIndices = getLineToStateIndices(oppositeHashes);

        Map<Integer, Set<Integer>> oppositeLineToStates = getLineToStates(oppositeHashes);

        ProgramStateDiff.UniqueStateSummary firstUniqueStateSummary = new ProgramStateDiff.UniqueStateSummary();
        for (int i = 0; i < hashes.size(); i++) {
            Pair<Integer, Integer> p = hashes.get(i);
            int lineNumber = p.getLeft(), stateHash = p.getRight();

            if (!lineMapping.containsKey(lineNumber)) continue;

            int oppositeLineNumber = lineMapping.get(lineNumber);

            if (!oppositeLineToStates.containsKey(oppositeLineNumber)
                    || !oppositeLineToStates.get(oppositeLineNumber).contains(stateHash)) {
                // this stateHash is not covered in the opposite version

                printIfNeeded(
                        "Unique state at line " + lineNumber, jsonStates.get(i).toString(), diffPrinter);

                if (firstUniqueStateSummary.getFirstUniqueStateHash() == null) {
                    firstUniqueStateSummary.setFirstUniqueStateHash(stateHash);
                    firstUniqueStateSummary.setFirstUniqueStateLine(lineNumber);
                }

                VarValsSet distinctVarVals = identifyDistinctBreakpointVarVal(
                        jsonStates.get(i),
                        lineToVars.get(lineNumber),
                        oppositeJsonStates,
                        oppositeLineToStateIndices.get(oppositeLineNumber));

                for (String varVal : distinctVarVals.getAllVals()) {
                    printIfNeeded("Unique state var-val at line " + lineNumber, varVal, diffPrinter);
                }

                String selectedVarVal = distinctVarVals.getSelectedVal();

                if (selectedVarVal != null && firstUniqueStateSummary.getFirstUniqueVarVal() == null) {
                    String executedTest = getMatchingTest(
                            jsonStates.get(i).getStackFrameContext().get(0).getStackTrace());
                    firstUniqueStateSummary.setDifferencingTest(executedTest);
                    firstUniqueStateSummary.setFirstUniqueVarValLine(lineNumber);
                    firstUniqueStateSummary.setFirstUniqueVarVal(selectedVarVal);
                    if (diffPrinter == null) break;
                }
            }
        }

        return firstUniqueStateSummary;
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

    private VarValsSet identifyDistinctReturnVarVal(
            RuntimeReturnedValue jsonState,
            List<RuntimeReturnedValue> oppositeJsonStates,
            List<Integer> oppositeTargetStateIndices)
            throws IOException {
        Set<String> distinctVarVals = extractVarVals("{return-object}", jsonState);

        if (oppositeTargetStateIndices != null)
            for (int ind : oppositeTargetStateIndices) {
                RuntimeReturnedValue oppositeState = oppositeJsonStates.get(ind);
                distinctVarVals.removeAll(extractVarVals("{return-object}", oppositeState));
            }

        String shortestDistinctVarVal = null;

        for (String varVal : distinctVarVals) {
            if (shortestDistinctVarVal == null) shortestDistinctVarVal = varVal;
            else {
                int shortestLen = shortestDistinctVarVal.length(),
                        shortestParts = shortestDistinctVarVal.split("=")[0].split(".").length,
                        curParts = varVal.split("=")[0].split(".").length,
                        curLen = varVal.length();

                if (shortestParts > curParts || (shortestParts == curParts && shortestLen > curLen))
                    shortestDistinctVarVal = varVal;
            }
        }

        return new VarValsSet(distinctVarVals, shortestDistinctVarVal);
    }

    private VarValsSet identifyDistinctBreakpointVarVal(
            LineSnapshot jsonState,
            Set<String> lineVars,
            List<LineSnapshot> oppositeJsonStates,
            List<Integer> oppositeTargetStateIndices)
            throws IOException {
        List<RuntimeValue> valueCollection = breakpointStateToValueCollection(jsonState);

        Set<String> distinctVarVals = extractVarVals(valueCollection, lineVars, true);

        if (oppositeTargetStateIndices != null)
            for (int ind : oppositeTargetStateIndices) {
                List<RuntimeValue> oppositeValueCollection =
                        breakpointStateToValueCollection(oppositeJsonStates.get(ind));
                distinctVarVals.removeAll(extractVarVals(oppositeValueCollection, lineVars, true));
            }

        String shortestDistinctVarVal = null;

        for (String varVal : distinctVarVals) {
            if (shortestDistinctVarVal == null) shortestDistinctVarVal = varVal;
            else {
                int shortestLen = shortestDistinctVarVal.length(),
                        shortestParts = shortestDistinctVarVal.split(".").length,
                        curParts = varVal.split(".").length,
                        curLen = varVal.length();

                if (shortestParts > curParts || (shortestParts == curParts && shortestLen > curLen))
                    shortestDistinctVarVal = varVal;
            }
        }

        return new VarValsSet(distinctVarVals, shortestDistinctVarVal);
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

    private List<RuntimeValue> breakpointStateToValueCollection(LineSnapshot state) {
        return state.getStackFrameContext().get(0).getRuntimeValueCollection();
    }

    // Gives Json indices that correspond to states of a line
    private Map<Integer, List<Integer>> getLineToStateIndices(List<Pair<Integer, Integer>> lineToHashes) {
        Map<Integer, List<Integer>> ret = new HashMap<>();

        for (int i = 0; i < lineToHashes.size(); i++) {
            int lineNumber = lineToHashes.get(i).getLeft();
            if (!ret.containsKey(lineNumber)) ret.put(lineNumber, new ArrayList<>());
            ret.get(lineNumber).add(i);
        }

        return ret;
    }

    private Map<Integer, Set<Integer>> getLineToStates(List<Pair<Integer, Integer>> lineToHashes) {
        Map<Integer, Set<Integer>> ret = new HashMap<>();

        for (Pair<Integer, Integer> p : lineToHashes) {
            int lineNumber = p.getLeft(), state = p.getRight();
            if (!ret.containsKey(lineNumber)) ret.put(lineNumber, new HashSet<>());
            ret.get(lineNumber).add(state);
        }

        return ret;
    }

    // returns pair of (lineNumber, stateHash)
    private List<Pair<Integer, Integer>> getHashedBreakpointStates(List<LineSnapshot> ja) {
        List<Pair<Integer, Integer>> hashedStates = new ArrayList<>();

        for (LineSnapshot jo : ja) {
            hashedStates.add(breakpointStateJsonToHashedStatePair(jo));
        }

        return hashedStates;
    }

    // Left of return is the lineNumber and right is the state hashhash
    private Pair<Integer, Integer> breakpointStateJsonToHashedStatePair(LineSnapshot stateJO) {
        int lineNumber = stateJO.getLineNumber();
        StackFrameContext stackFrameContextJO = stateJO.getStackFrameContext().get(0);
        return new Pair<>(
                lineNumber,
                stackFrameContextJO.getRuntimeValueCollection().toString().hashCode());
    }

    private Pair<Integer, Integer> returnStateJsonToHash(RuntimeReturnedValue stateJO) {
        int lineNumber = Integer.parseInt(stateJO.getLocation().split(":")[1]);
        return new Pair<>(
                lineNumber, ("" + stateJO.getFields() + stateJO.getValue() + stateJO.getArrayElements()).hashCode());
    }
}
