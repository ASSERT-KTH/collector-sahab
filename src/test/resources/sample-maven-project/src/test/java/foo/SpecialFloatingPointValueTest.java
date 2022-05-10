package foo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import foo.SpecialFloatingPointValue;

public class SpecialFloatingPointValueTest {
    @Test
    void test_generateNaN() {
        assertTrue(Double.isNaN(SpecialFloatingPointValue.generateNaN()));
    }
}
