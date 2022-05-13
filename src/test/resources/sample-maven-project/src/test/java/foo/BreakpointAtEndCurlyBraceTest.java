package foo;

import org.junit.jupiter.api.Test;
import foo.breakpointAtEndCurlyBrace.NonVoid;
import foo.breakpointAtEndCurlyBrace.Void;

public class BreakpointAtEndCurlyBraceTest {
    @Test
    void test_shouldEndLineBeCollected_nonVoid() {
        NonVoid.shouldEndLineBeCollected();
    }

    @Test
    void test_doNotReturnAnything_void() {
        Void.doNotReturnAnything();
    }
}
