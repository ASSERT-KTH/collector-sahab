package foo;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import foo.RecordMyReturnButWithoutBreakpoints;

public class RecordMyReturnButWithoutBreakpointsTest {
    @Test
    void abba() {
        String x = RecordMyReturnButWithoutBreakpoints.gimmegimmegimme();
        assertFalse(x.isEmpty());
    }
}
