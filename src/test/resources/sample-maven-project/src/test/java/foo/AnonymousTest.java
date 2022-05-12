package foo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import foo.anonymous.Clazz;
import foo.anonymous.Lambda;

public class AnonymousTest {
    @Test
    void test_implementAnonymousGreetings() {
        Clazz.implementAnonymousGreetings();
        assertTrue(true);
    }

    @Test
    void test_printString() {
        assertEquals("Hey!", Lambda.printString());
    }
}
