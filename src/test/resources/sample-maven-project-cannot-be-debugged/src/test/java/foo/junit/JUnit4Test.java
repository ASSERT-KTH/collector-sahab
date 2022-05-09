package foo.junit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JUnit4Test {
    @Test
    public void test_concat() {
        String a = "aman";
        String b = "sharma";
        assertEquals("amansharma", a.concat(b));
    }

    @Test
    public void test_upperCase() {
        String a = "foo";
        assertEquals("FOO", a.toUpperCase());
    }
}
