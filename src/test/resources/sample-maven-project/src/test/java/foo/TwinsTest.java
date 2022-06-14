package foo;

import org.junit.jupiter.api.Test;
import foo.twins.*;

public class TwinsTest {
    @Test
    void executeBothMethods() {
        A.getValue();
        B.getValue();
        assert true;
    }
}
