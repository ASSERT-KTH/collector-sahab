import org.junit.jupiter.api.Test;
import synthetic.SyntheticField;
import synthetic.SyntheticMethod;

class SyntheticTest {

    @Test
    void fieldTest() {
        SyntheticField.InnerClass rizz = new SyntheticField().new InnerClass();
        assert rizz.getZ() == 42;
    }

    @Test
    void methodTest() {
        SyntheticMethod rizz = new SyntheticMethod();
        assert rizz.getX() == 42;
    }
}