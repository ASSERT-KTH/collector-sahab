package io.github.chains_project.tracediff.computer;

import static org.junit.jupiter.api.Assertions.*;

import io.github.chains_project.tracediff.statediff.computer.StateDiffComputer;
import io.github.chains_project.tracediff.statediff.models.ProgramStateDiff;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class StateDiffComputerTest<R> {

    // data from https://github.com/khaes-kth/drr-execdiff/commit/649e9234549d2c74f1279499011da87638c2d718, depth=3
    @Test
    void computeStateDiff_simple_diffIsGenerated() throws IOException {
        Path simpleSahabDirectory = Paths.get("src/test/resources/sahab_reports/simple");
        File leftSahabReport = simpleSahabDirectory.resolve("report/left.json").toFile(),
                rightSahabReport =
                        simpleSahabDirectory.resolve("report/right.json").toFile(),
                lineMapping =
                        simpleSahabDirectory
                                .resolve("project_data/line_mapping.csv")
                                .toFile(),
                leftLineToVarFile =
                        simpleSahabDirectory
                                .resolve("project_data/left_line_to_var.csv")
                                .toFile(),
                rightLineToVarFile =
                        simpleSahabDirectory
                                .resolve("project_data/right_line_to_var.csv")
                                .toFile();

        Map<Integer, Set<String>> leftLineToVars = readLineVarsFromFile(leftLineToVarFile),
                rightLineToVars = readLineVarsFromFile(rightLineToVarFile);

        Pair<Map<Integer, Integer>, Map<Integer, Integer>> mappings = readMappingsFromFile(lineMapping);

        StateDiffComputer sdc = new StateDiffComputer(
                leftSahabReport,
                rightSahabReport,
                mappings.getLeft(),
                mappings.getRight(),
                leftLineToVars,
                rightLineToVars,
                List.of("test::test"));

        ProgramStateDiff stateDiff = sdc.computeProgramStateDiff(null, false);

        assertEquals(
                "{return-object}.UTC.iID=UTC",
                stateDiff.getOriginalUniqueReturn().getFirstUniqueVarVal());
        assertEquals("{return-object}=null", stateDiff.getPatchedUniqueReturn().getFirstUniqueVarVal());

        assertEquals(
                "iRules.size=2", stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarVal());
        assertNull(stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarVal());
    }

    // breakpoint from: https://github.com/khaes-kth/drr-execdiff/commit/1c04679173a46faa59e73f68def33f60843f8beb
    // only a part of breakpoint data is stored in right.json
    @Test
    void computeStateDiff_complex_diffIsGenerated() throws IOException {
        Path simpleSahabDirectory = Paths.get("src/test/resources/sahab_reports/complex");
        File leftSahabReport = simpleSahabDirectory.resolve("report/left.json").toFile(),
                rightSahabReport =
                        simpleSahabDirectory.resolve("report/right.json").toFile(),
                lineMapping =
                        simpleSahabDirectory
                                .resolve("project_data/line_mapping.csv")
                                .toFile(),
                leftLineToVarFile =
                        simpleSahabDirectory
                                .resolve("project_data/left_line_to_var.csv")
                                .toFile(),
                rightLineToVarFile =
                        simpleSahabDirectory
                                .resolve("project_data/right_line_to_var.csv")
                                .toFile();

        Map<Integer, Set<String>> leftLineToVars = readLineVarsFromFile(leftLineToVarFile),
                rightLineToVars = readLineVarsFromFile(rightLineToVarFile);

        Pair<Map<Integer, Integer>, Map<Integer, Integer>> mappings = readMappingsFromFile(lineMapping);

        StateDiffComputer sdc = new StateDiffComputer(
                leftSahabReport,
                rightSahabReport,
                mappings.getLeft(),
                mappings.getRight(),
                leftLineToVars,
                rightLineToVars,
                List.of("test::test"));

        ProgramStateDiff stateDiff = sdc.computeProgramStateDiff(null, false);

        assertEquals(
                "tailZone.iID=TestDTZ1",
                stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarVal());
        assertNull(stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarVal());
    }

    // breakpoint from: https://github.com/khaes-kth/drr-execdiff/commit/8b5b580751d1c08eb848e389ec3e7e235eea62d8,
    // depth=1
    // only a part of breakpoint data is stored in right.json
    @Test
    void computeStateDiff_simple_diffIsGenerated_two() throws IOException {
        Path simpleSahabDirectory = Paths.get("src/test/resources/sahab_reports/simple_two");
        File leftSahabReport = simpleSahabDirectory.resolve("report/left.json").toFile(),
                rightSahabReport =
                        simpleSahabDirectory.resolve("report/right.json").toFile(),
                lineMapping =
                        simpleSahabDirectory
                                .resolve("project_data/line_mapping.csv")
                                .toFile(),
                leftLineToVarFile =
                        simpleSahabDirectory
                                .resolve("project_data/left_line_to_var.csv")
                                .toFile(),
                rightLineToVarFile =
                        simpleSahabDirectory
                                .resolve("project_data/right_line_to_var.csv")
                                .toFile();

        Map<Integer, Set<String>> leftLineToVars = readLineVarsFromFile(leftLineToVarFile),
                rightLineToVars = readLineVarsFromFile(rightLineToVarFile);

        Pair<Map<Integer, Integer>, Map<Integer, Integer>> mappings = readMappingsFromFile(lineMapping);

        StateDiffComputer sdc = new StateDiffComputer(
                leftSahabReport,
                rightSahabReport,
                mappings.getLeft(),
                mappings.getRight(),
                leftLineToVars,
                rightLineToVars,
                List.of("test::test"));

        ProgramStateDiff stateDiff = sdc.computeProgramStateDiff(null, false);

        assertEquals(
                "id=TestDTZ1", stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarVal());
        assertEquals(
                "next.iWallOffset=3600000",
                stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarVal());
    }

    private Map<Integer, Set<String>> readLineVarsFromFile(File lineVarsFile) throws IOException {
        Map<Integer, Set<String>> lineToVars = new HashMap<>();
        FileUtils.readLines(lineVarsFile, "UTF-8").forEach(l -> {
            lineToVars.put(Integer.parseInt(l.split(",")[0]), new HashSet<>(List.of(l.split(",")[1].split(";"))));
        });

        return lineToVars;
    }

    // Pair<left-right, right-left> mappings
    private Pair<Map<Integer, Integer>, Map<Integer, Integer>> readMappingsFromFile(File mappingFile)
            throws IOException {
        Map<Integer, Integer> leftRightMapping = new HashMap<>();
        Map<Integer, Integer> rightLeftMapping = new HashMap<>();
        List<String> lines = FileUtils.readLines(mappingFile, "UTF-8");
        lines.forEach(l -> leftRightMapping.put(Integer.parseInt(l.split(",")[0]), Integer.parseInt(l.split(",")[1])));
        lines.forEach(l -> rightLeftMapping.put(Integer.parseInt(l.split(",")[1]), Integer.parseInt(l.split(",")[0])));
        return Pair.of(leftRightMapping, rightLeftMapping);
    }
}
