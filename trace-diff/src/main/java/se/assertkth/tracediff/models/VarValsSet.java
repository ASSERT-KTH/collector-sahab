package se.assertkth.tracediff.models;

import java.util.Set;

public class VarValsSet {
    private Set<String> allVals;
    private String selectedVal;

    public VarValsSet(Set<String> allVals, String selectedVal) {
        this.allVals = allVals;
        this.selectedVal = selectedVal;
    }

    public Set<String> getAllVals() {
        return allVals;
    }

    public void setAllVals(Set<String> allVals) {
        this.allVals = allVals;
    }

    public String getSelectedVal() {
        return selectedVal;
    }

    public void setSelectedVal(String selectedVal) {
        this.selectedVal = selectedVal;
    }
}
