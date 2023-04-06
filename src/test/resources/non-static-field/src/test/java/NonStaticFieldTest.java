import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NonStaticFieldTest {
    @Test
    void test_doSomething() {
        assertTrue(new NonStaticField().doSomething());
    }
}
