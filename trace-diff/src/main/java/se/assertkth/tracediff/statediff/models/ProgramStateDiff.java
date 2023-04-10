package se.assertkth.tracediff.statediff.models;

public class ProgramStateDiff {
    private UniqueStateSummary firstOriginalUniqueStateSummary, firstPatchedUniqueStateSummary;
    private UniqueReturnSummary originalUniqueReturn, patchedUniqueReturn;

    public UniqueStateSummary getFirstOriginalUniqueStateSummary() {
        return firstOriginalUniqueStateSummary;
    }

    public void setFirstOriginalUniqueStateSummary(UniqueStateSummary firstOriginalUniqueStateSummary) {
        this.firstOriginalUniqueStateSummary = firstOriginalUniqueStateSummary;
    }

    public UniqueStateSummary getFirstPatchedUniqueStateSummary() {
        return firstPatchedUniqueStateSummary;
    }

    public void setFirstPatchedUniqueStateSummary(UniqueStateSummary firstPatchedUniqueStateSummary) {
        this.firstPatchedUniqueStateSummary = firstPatchedUniqueStateSummary;
    }

    public UniqueReturnSummary getOriginalUniqueReturn() {
        return originalUniqueReturn;
    }

    public void setOriginalUniqueReturn(UniqueReturnSummary originalUniqueReturn) {
        this.originalUniqueReturn = originalUniqueReturn;
    }

    public UniqueReturnSummary getPatchedUniqueReturn() {
        return patchedUniqueReturn;
    }

    public void setPatchedUniqueReturn(UniqueReturnSummary patchedUniqueReturn) {
        this.patchedUniqueReturn = patchedUniqueReturn;
    }

    public static class UniqueReturnSummary {
        private Integer firstUniqueVarValLine, firstUniqueReturnLine, firstUniqueReturnHash;
        private String firstUniqueVarVal, differencingTest;

        public Integer getFirstUniqueVarValLine() {
            return firstUniqueVarValLine;
        }

        public void setFirstUniqueVarValLine(Integer firstUniqueVarValLine) {
            this.firstUniqueVarValLine = firstUniqueVarValLine;
        }

        public String getFirstUniqueVarVal() {
            return firstUniqueVarVal;
        }

        public void setFirstUniqueVarVal(String firstUniqueVarVal) {
            this.firstUniqueVarVal = firstUniqueVarVal;
        }

        public Integer getFirstUniqueReturnHash() {
            return firstUniqueReturnHash;
        }

        public void setFirstUniqueReturnHash(Integer firstUniqueReturnHash) {
            this.firstUniqueReturnHash = firstUniqueReturnHash;
        }

        public Integer getFirstUniqueReturnLine() {
            return firstUniqueReturnLine;
        }

        public void setFirstUniqueReturnLine(Integer firstUniqueReturnLine) {
            this.firstUniqueReturnLine = firstUniqueReturnLine;
        }

        @Override
        public String toString() {
            return "UniqueReturnSummary{" +
                    "differencingTest=" + differencingTest +
                    ", firstUniqueVarValLine=" + firstUniqueVarValLine +
                    ", firstUniqueReturnLine=" + firstUniqueReturnLine +
                    ", firstUniqueReturnHash=" + firstUniqueReturnHash +
                    ", firstUniqueVarVal='" + firstUniqueVarVal + '\'' +
                    '}';
        }

        public String getDifferencingTest() {
            return differencingTest;
        }

        public void setDifferencingTest(String differencingTest) {
            this.differencingTest = differencingTest;
        }
    }

    public static class UniqueStateSummary {
        private Integer firstUniqueStateLine, firstUniqueVarValLine, firstUniqueStateHash;
        private String firstUniqueVarVal, differencingTest;

        public Integer getFirstUniqueStateLine() {
            return firstUniqueStateLine;
        }

        public void setFirstUniqueStateLine(Integer firstUniqueStateLine) {
            this.firstUniqueStateLine = firstUniqueStateLine;
        }

        public Integer getFirstUniqueVarValLine() {
            return firstUniqueVarValLine;
        }

        public void setFirstUniqueVarValLine(Integer firstUniqueVarValLine) {
            this.firstUniqueVarValLine = firstUniqueVarValLine;
        }

        public Integer getFirstUniqueStateHash() {
            return firstUniqueStateHash;
        }

        public void setFirstUniqueStateHash(Integer firstUniqueStateHash) {
            this.firstUniqueStateHash = firstUniqueStateHash;
        }

        public String getFirstUniqueVarVal() {
            return firstUniqueVarVal;
        }

        public void setFirstUniqueVarVal(String firstUniqueVarVal) {
            this.firstUniqueVarVal = firstUniqueVarVal;
        }

        @Override
        public String toString() {
            return "UniqueStateSummary{" +
                    "differencingTest=" + differencingTest +
                    ", firstUniqueStateLine=" + firstUniqueStateLine +
                    ", firstUniqueVarValLine=" + firstUniqueVarValLine +
                    ", firstUniqueStateHash=" + firstUniqueStateHash +
                    ", firstUniqueVarVal='" + firstUniqueVarVal + '\'' +
                    '}';
        }

        public String getDifferencingTest() {
            return differencingTest;
        }

        public void setDifferencingTest(String differencingTest) {
            this.differencingTest = differencingTest;
        }
    }

    @Override
    public String toString() {
        return "ProgramStateDiff{" +
                "firstOriginalUniqueStateSummary=" + firstOriginalUniqueStateSummary +
                ", firstPatchedUniqueStateSummary=" + firstPatchedUniqueStateSummary +
                ", originalUniqueReturn=" + originalUniqueReturn +
                ", patchedUniqueReturn=" + patchedUniqueReturn +
                '}';
    }
}
