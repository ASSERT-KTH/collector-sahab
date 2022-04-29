package foo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import foo.objects.OneLevelNestedObject;

public class ObjectsTest {
    @Test
    void justOneLevel() {
        OneLevelNestedObject o = new OneLevelNestedObject();
        assertTrue(o.getATriangle() instanceof Object);
    }
}
