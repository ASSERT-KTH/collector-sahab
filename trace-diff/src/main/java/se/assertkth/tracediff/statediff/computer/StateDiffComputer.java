package se.assertkth.tracediff.statediff.computer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import se.assertkth.tracediff.Constants;
import se.assertkth.tracediff.models.VarValsSet;
import se.assertkth.tracediff.statediff.models.ProgramStateDiff;

public class StateDiffComputer {
    private File leftSahabReport, rightSahabReport;
    private Map<Integer, Integer> leftRightLineMapping, rightLeftLineMapping;
    private Map<Integer, Set<String>> leftLineToVars, rightLineToVars;
    private List<String> tests;

    public StateDiffComputer(
            File leftSahabReport,
            File rightSahabReport,
            Map<Integer, Integer> leftRightLineMapping,
            Map<Integer, Integer> rightLeftLineMapping,
            Map<Integer, Set<String>> leftLineToVars,
            Map<Integer, Set<String>> rightLineToVars,
            List<String> tests)
            throws IOException {
        this.leftSahabReport = leftSahabReport;
        this.rightSahabReport = rightSahabReport;
        this.leftLineToVars = leftLineToVars;
        this.rightLineToVars = rightLineToVars;
        this.leftRightLineMapping = leftRightLineMapping;
        this.rightLeftLineMapping = rightLeftLineMapping;
        this.tests = tests;
    }

    public ProgramStateDiff computeProgramStateDiff(PrintWriter diffPrinter) throws IOException, ParseException {
        ProgramStateDiff programStateDiff = new ProgramStateDiff();

        programStateDiff.setFirstOriginalUniqueStateSummary(getFirstDistinctStateOnRelevantLine(
                leftLineToVars, leftRightLineMapping, leftSahabReport, rightSahabReport, diffPrinter));

        programStateDiff.setFirstPatchedUniqueStateSummary(getFirstDistinctStateOnRelevantLine(
                rightLineToVars, rightLeftLineMapping, rightSahabReport, leftSahabReport, diffPrinter));

        programStateDiff.setOriginalUniqueReturn(
                getFirstUniqueReturn(leftRightLineMapping, leftSahabReport, rightSahabReport, diffPrinter));

        programStateDiff.setPatchedUniqueReturn(
                getFirstUniqueReturn(rightLeftLineMapping, rightSahabReport, leftSahabReport, diffPrinter));

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
            File sahabReportFile,
            File oppositeSahabReportFile,
            PrintWriter diffPrinter)
            throws IOException, ParseException {

        JSONParser parser = new JSONParser();
        JSONArray jsonStates = (JSONArray) ((JSONObject) parser.parse(new FileReader(sahabReportFile))).get("return"),
                oppositeJsonStates =
                        (JSONArray) ((JSONObject) parser.parse(new FileReader(oppositeSahabReportFile))).get("return");

        List<Pair<Integer, Integer>> hashes = getHashedReturnStates(jsonStates),
                oppositeHashes = getHashedReturnStates(oppositeJsonStates);

        Map<Integer, List<Integer>> oppositeLineToStateIndices = getLineToStateIndices(oppositeHashes);

        Map<Integer, Set<Integer>> oppositeLineToStates = getLineToStates(oppositeHashes);

        ProgramStateDiff.UniqueReturnSummary firstUniqueReturnSummary = new ProgramStateDiff.UniqueReturnSummary();

        for (int i = 0; i < hashes.size(); i++) {
            Pair<Integer, Integer> p = hashes.get(i);
            int lineNumber = p.getKey(), stateHash = p.getValue();

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
                        (JSONObject) jsonStates.get(i),
                        oppositeJsonStates,
                        oppositeLineToStateIndices.get(oppositeLineNumber));

                for (String varVal : distinctVarVals.getAllVals()) {
                    printIfNeeded("Unique return var-val at line " + lineNumber, varVal, diffPrinter);
                }

                String selectedVarVal = distinctVarVals.getSelectedVal();

                if (selectedVarVal != null && firstUniqueReturnSummary.getFirstUniqueVarVal() == null) {
                    String executedTest =
                            getMatchingTest((JSONArray) ((JSONObject) jsonStates.get(i)).get("stackTrace"));
                    firstUniqueReturnSummary.setDifferencingTest(executedTest);
                    firstUniqueReturnSummary.setFirstUniqueVarValLine(lineNumber);
                    firstUniqueReturnSummary.setFirstUniqueVarVal(selectedVarVal);

                    if (diffPrinter == null) break;
                }
            }
        }

        return firstUniqueReturnSummary;
    }

    private List<Pair<Integer, Integer>> getHashedReturnStates(JSONArray ja) {
        List<Pair<Integer, Integer>> hashes = new ArrayList<>();

        for (int i = 0; i < ja.size(); i++) {
            JSONObject jo = (JSONObject) ja.get(i);
            hashes.add(returnStateJsonToHash(jo));
        }

        return hashes;
    }

    // return first state in @hashes that does not exist in states of the corresponding opposite line
    private ProgramStateDiff.UniqueStateSummary getFirstDistinctStateOnRelevantLine(
            Map<Integer, Set<String>> lineToVars,
            Map<Integer, Integer> lineMapping,
            File sahabReportFile,
            File oppositeSahabReportFile,
            PrintWriter diffPrinter)
            throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONArray
                jsonStates = (JSONArray) ((JSONObject) parser.parse(new FileReader(sahabReportFile))).get("breakpoint"),
                oppositeJsonStates =
                        (JSONArray)
                                ((JSONObject) parser.parse(new FileReader(oppositeSahabReportFile))).get("breakpoint");

        List<Pair<Integer, Integer>> hashes = getHashedBreakpointStates(jsonStates),
                oppositeHashes = getHashedBreakpointStates(oppositeJsonStates);

        Map<Integer, List<Integer>> oppositeLineToStateIndices = getLineToStateIndices(oppositeHashes);

        Map<Integer, Set<Integer>> oppositeLineToStates = getLineToStates(oppositeHashes);

        ProgramStateDiff.UniqueStateSummary firstUniqueStateSummary = new ProgramStateDiff.UniqueStateSummary();
        for (int i = 0; i < hashes.size(); i++) {
            Pair<Integer, Integer> p = hashes.get(i);
            int lineNumber = p.getKey(), stateHash = p.getValue();

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
                        (JSONObject) jsonStates.get(i),
                        lineToVars.get(lineNumber),
                        oppositeJsonStates,
                        oppositeLineToStateIndices.get(oppositeLineNumber));

                for (String varVal : distinctVarVals.getAllVals()) {
                    printIfNeeded("Unique state var-val at line " + lineNumber, varVal, diffPrinter);
                }

                String selectedVarVal = distinctVarVals.getSelectedVal();

                if (selectedVarVal != null && firstUniqueStateSummary.getFirstUniqueVarVal() == null) {
                    String executedTest = getMatchingTest((JSONArray) ((JSONObject)
                                    ((JSONArray) ((JSONObject) jsonStates.get(i)).get("stackFrameContext")).get(0))
                            .get("stackTrace"));
                    firstUniqueStateSummary.setDifferencingTest(executedTest);
                    firstUniqueStateSummary.setFirstUniqueVarValLine(lineNumber);
                    firstUniqueStateSummary.setFirstUniqueVarVal(selectedVarVal);
                    if (diffPrinter == null) break;
                }
            }
        }

        return firstUniqueStateSummary;
    }

    private String getMatchingTest(JSONArray stackTraceJa) {
        if (tests.size() == 1) return tests.get(0);

        for (String test : tests) {
            String[] testParts = test.split(Constants.TEST_METHOD_NAME_SEPARATOR);
            String testClass = testParts[0], testMethod = testParts.length > 1 ? testParts[1] : null;

            for (int i = 0; i < stackTraceJa.size(); i++) {
                String stackItem = stackTraceJa.get(i).toString();
                if (stackItem.contains(testClass) && (testMethod != null && stackItem.contains(testMethod)))
                    return test;
            }
        }
        return Constants.UNKNOWN_TEST;
    }

    private VarValsSet identifyDistinctReturnVarVal(
            JSONObject jsonState, JSONArray oppositeJsonStates, List<Integer> oppositeTargetStateIndices)
            throws IOException {
        Set<String> distinctVarVals = extractVarVals("{return-object}", jsonState);

        if (oppositeTargetStateIndices != null)
            for (int ind : oppositeTargetStateIndices) {
                JSONObject oppositeState = ((JSONObject) oppositeJsonStates.get(ind));
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
            JSONObject jsonState,
            Set<String> lineVars,
            JSONArray oppositeJsonStates,
            List<Integer> oppositeTargetStateIndices)
            throws IOException {
        JSONArray valueCollection = breakpointStateToValueCollection(jsonState);

        Set<String> distinctVarVals = extractVarVals(valueCollection, lineVars, true);

        if (oppositeTargetStateIndices != null)
            for (int ind : oppositeTargetStateIndices) {
                JSONArray oppositeValueCollection =
                        breakpointStateToValueCollection((JSONObject) oppositeJsonStates.get(ind));
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

    private Set<String> extractVarVals(JSONArray valueCollection, Set<String> lineVarsLst, boolean checkLineVars)
            throws IOException {
        Set<String> varVals = new HashSet<>();

        for (int i = 0; i < valueCollection.size(); i++) {
            JSONObject valueJo = (JSONObject) valueCollection.get(i);

            // a variable that is not important in this line should be ignored
            if (checkLineVars
                    && (lineVarsLst == null
                            || !lineVarsLst.contains(valueJo.get("name").toString()))) continue;

            varVals.addAll(extractVarVals("", valueJo));
        }
        return varVals;
    }

    private Set<String> extractVarVals(String prefix, JSONObject valueJo) throws IOException {
        Set<String> varVals = new HashSet<>();
        if (valueJo.get("fields") != null && ((JSONArray) valueJo.get("fields")).size() > 0) {
            prefix += (valueJo.containsKey("name") ? valueJo.get("name") : "").toString() + ".";
            JSONArray nestedTypes = (JSONArray) valueJo.get("fields");

            for (int i = 0; i < nestedTypes.size(); i++) {
                JSONObject nestedObj = (JSONObject) nestedTypes.get(i);
                varVals.addAll(extractVarVals(prefix, nestedObj));
            }
        } else if (valueJo.get("arrayElements") != null && ((JSONArray) valueJo.get("arrayElements")).size() > 0) {
            JSONArray nestedTypes = (JSONArray) valueJo.get("arrayElements");

            prefix += (valueJo.containsKey("name") ? valueJo.get("name") : "").toString();

            for (int i = 0; i < nestedTypes.size(); i++) {
                JSONObject nestedObj = (JSONObject) nestedTypes.get(i);
                String currentPrefix = prefix + "[" + i + "]";
                varVals.addAll(extractVarVals(currentPrefix, nestedObj));
            }
        } else {
            // it's a leaf node
            String currentPrefix = prefix + (valueJo.containsKey("name") ? valueJo.get("name") : "");
            String value = valueJo.get("value") + "";
            varVals.add(currentPrefix + "=" + value);
        }

        return varVals;
    }

    private JSONArray breakpointStateToValueCollection(JSONObject state) {
        return (JSONArray)
                ((JSONObject) ((JSONArray) state.get("stackFrameContext")).get(0)).get("runtimeValueCollection");
    }

    // Gives Json indices that correspond to states of a line
    private Map<Integer, List<Integer>> getLineToStateIndices(List<Pair<Integer, Integer>> lineToHashes) {
        Map<Integer, List<Integer>> ret = new HashMap<>();

        for (int i = 0; i < lineToHashes.size(); i++) {
            int lineNumber = lineToHashes.get(i).getKey();
            if (!ret.containsKey(lineNumber)) ret.put(lineNumber, new ArrayList<>());
            ret.get(lineNumber).add(i);
        }

        return ret;
    }

    private Map<Integer, Set<Integer>> getLineToStates(List<Pair<Integer, Integer>> lineToHashes) {
        Map<Integer, Set<Integer>> ret = new HashMap<>();

        for (Pair<Integer, Integer> p : lineToHashes) {
            int lineNumber = p.getKey(), state = p.getValue();
            if (!ret.containsKey(lineNumber)) ret.put(lineNumber, new HashSet<>());
            ret.get(lineNumber).add(state);
        }

        return ret;
    }

    // returns pair of (lineNumber, stateHash)
    private List<Pair<Integer, Integer>> getHashedBreakpointStates(JSONArray ja) throws IOException, ParseException {
        List<Pair<Integer, Integer>> hashedStates = new ArrayList<>();

        for (int i = 0; i < ja.size(); i++) {
            JSONObject jo = (JSONObject) ja.get(i);
            hashedStates.add(breakpointStateJsonToHashedStatePair(jo));
        }

        return hashedStates;
    }

    // Left of return is the lineNumber and right is the state hashhash
    private Pair<Integer, Integer> breakpointStateJsonToHashedStatePair(JSONObject stateJO) {
        int lineNumber = Integer.parseInt(stateJO.get("lineNumber").toString());
        stateJO = (JSONObject) ((JSONArray) stateJO.get("stackFrameContext")).get(0);
        return Pair.of(
                lineNumber, stateJO.get("runtimeValueCollection").toString().hashCode());
    }

    private Pair<Integer, Integer> returnStateJsonToHash(JSONObject stateJO) {
        int lineNumber = Integer.parseInt(stateJO.get("location").toString().split(":")[1]);
        return Pair.of(
                lineNumber,
                ("" + stateJO.get("fields") + stateJO.get("value") + stateJO.get("arrayElements")).hashCode());
    }
}
