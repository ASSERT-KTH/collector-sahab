package foo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import foo.objects.OneLevelNestedObject;
import foo.objects.MultipleLevelNestedObject;

public class ObjectsTest {
    @Test
    void justOneLevel() {
        OneLevelNestedObject o = new OneLevelNestedObject();
        assertTrue(o.getATriangle() instanceof Object);
    }

    @Test
    void maybeTwoMoreLevels() {
        MultipleLevelNestedObject m = new MultipleLevelNestedObject();
        assertEquals(42, m.meaningOfLife());
    }
}
