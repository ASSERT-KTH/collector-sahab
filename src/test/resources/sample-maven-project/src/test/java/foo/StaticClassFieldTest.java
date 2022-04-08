package foo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import foo.StaticClassField;

public class StaticClassFieldTest {
    @Test
    void test_doSomething() {
        assertTrue(new StaticClassField().doSomething());
    }
}
