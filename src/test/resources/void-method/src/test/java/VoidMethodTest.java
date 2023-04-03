import org.junit.jupiter.api.Test;

public class VoidMethodTest {
    @Test
    void test_doNotReturnAnything_void() {
        VoidMethod.doNotReturnAnything();
    }

    @Test
    void test_doNotReturnAnything_boxedVoid() {
        BoxedVoidMethod.doNotReturnAnything();
    }
}
