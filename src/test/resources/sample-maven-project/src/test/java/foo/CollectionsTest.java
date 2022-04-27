package foo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import foo.collections.NestedArray;
import foo.collections.OneLevelCollections;
import foo.collections.Primitive;

public class CollectionsTest {
    @Test
    void test_returnTruthy() {
        assertTrue(OneLevelCollections.returnTruthy());
    }

    @Test
    void test_canNestedArrayBeRepresented() {
        assertTrue(NestedArray.canNestedArrayBeRepresented());
    }

    @Test
    void test_canWePrintPrimitive() {
        Primitive.canWePrintPrimitive();
        assertTrue(true);
    }
}
