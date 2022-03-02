package se.kth.debug;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AppTest {

    @Test
    public void testAdd() {
        assertEquals(100, App.add(1, 2));
    }
}
