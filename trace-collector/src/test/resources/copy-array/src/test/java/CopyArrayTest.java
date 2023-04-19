import org.junit.jupiter.api.Test;

import foo.NonPrimitive;
import foo.Primitive;

public class CopyArrayTest {
    @Test
    void nonPrimitive() {
        NonPrimitive.setISOChronologyArray();
        assert true;
    }

    @Test
    void primitive() {
        Primitive.main();
        assert true;
    }
}
