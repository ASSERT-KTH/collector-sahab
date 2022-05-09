package foo.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JUnit5Test {
    @Test
    void test_add() {
        assertEquals(4, 2+2);
    }

    @Test
    void test_subtract() {
        assertEquals(0, 2-2);
    }
}
