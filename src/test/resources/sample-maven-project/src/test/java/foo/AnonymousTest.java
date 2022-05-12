package foo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import foo.anonymous.Clazz;

public class AnonymousTest {
    @Test
    void test_implementAnonymousGreetings() {
        Clazz.implementAnonymousGreetings();
        assertTrue(true);
    }
}
