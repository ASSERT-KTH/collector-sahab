package se.assertteam.runtime.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import se.assertteam.runtime.LineSnapshot;
import se.assertteam.runtime.RuntimeReturnedValue;

public class SahabOutput {

    private final List<LineSnapshot> breakpoint;
    private final List<RuntimeReturnedValue> returns;

    public SahabOutput() {
        this.breakpoint = Collections.synchronizedList(new ArrayList<>());
        this.returns = Collections.synchronizedList(new ArrayList<>());
    }

    public List<LineSnapshot> getBreakpoint() {
        return breakpoint;
    }

    @JsonProperty("return")
    public List<RuntimeReturnedValue> getReturns() {
        return returns;
    }
}
