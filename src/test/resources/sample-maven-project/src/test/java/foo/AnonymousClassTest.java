package foo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import foo.AnonymousClass;

public class AnonymousClassTest {
    @Test
    void test_implementAnonymousGreetings() {
        AnonymousClass.implementAnonymousGreetings();
        assertTrue(true);
    }
}
