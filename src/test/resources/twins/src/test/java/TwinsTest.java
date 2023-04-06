import org.junit.jupiter.api.Test;
import foo.*;

public class TwinsTest {
    @Test
    void executeBothMethods() {
        A.getValue();
        B.getValue();
        assert true;
    }
}
