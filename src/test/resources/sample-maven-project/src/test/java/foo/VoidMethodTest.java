package foo;

import org.junit.jupiter.api.Test;

import foo.VoidMethod;

public class VoidMethodTest {
    @Test
    void test_doNothing() {
        VoidMethod vm = new VoidMethod();
        vm.doNothing();
        assert true;
    }
}
