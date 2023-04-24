import org.junit.jupiter.api.Test;

import java.util.Arrays;

class ArrayValueTest {
    @Test
    public void test() {
        int[] array = new int[42];
        Arrays.fill(array, 42);
        assert array[0] == 42;
        assert array[41] == 42;
    }
}
