import gas.Volatile;
import org.junit.jupiter.api.Test;

class VolatileTest {
    @Test
    void test() {
        Volatile vNonNull = new Volatile();
        Volatile vNull = null;
        if (vNull == null) {
            System.out.println("vNull is null");
        }
    }
}
