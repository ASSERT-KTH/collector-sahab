package foo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import foo.BasicMath;

public class NestedTest {

    @Nested
    class NestedClass {
        @Test
        void test_add() {
            assertEquals(2, BasicMath.add(2,1));
        }
    }
}
