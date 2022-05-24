package foo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


import foo.SwitchCase;
import org.junit.jupiter.api.Test;

public class SwitchCaseTest {
    @Test
    void test() {
        assertTrue(SwitchCase.isOdd(5));
        assertFalse(SwitchCase.isOdd(6));
        assertTrue(SwitchCase.isOdd(1));
    }
}
