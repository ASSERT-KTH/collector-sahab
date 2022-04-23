package foo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import foo.collections.OneLevelCollections;
import foo.collections.NestedCollections;

public class CollectionsTest {
    @Test
    void test_returnTruthy() {
        assertTrue(OneLevelCollections.returnTruthy());
    }

    @Test
    void test_returnFalsy() {
        assertFalse(NestedCollections.returnFalsy());
    }
}
