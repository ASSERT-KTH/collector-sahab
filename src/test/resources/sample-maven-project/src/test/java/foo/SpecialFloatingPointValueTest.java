package foo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import foo.SpecialFloatingPointValue;

public class SpecialFloatingPointValueTest {
    @Test
    void test_generateNaN() {
        assertTrue(Double.isNaN(SpecialFloatingPointValue.generateNaN()));
    }

    @Test
    void test_generateNaNFromFloat() {
        assertTrue(Float.isNaN(SpecialFloatingPointValue.generateNaNFromFloat(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)));
    }
}
