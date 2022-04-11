package foo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import foo.PrintingCollections;

public class PrintingCollectionsTest {
    @Test
    void test_returnTruthy() {
        assertTrue(PrintingCollections.returnTruthy());
    }
}
