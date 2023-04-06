package foo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DeathAndGloryTest {
    @Test
    void test_deathAndGlory() {
        assertEquals(42, DeathAndGlory.deathAndGlory(21));
        assertEquals(42, DeathAndGlory.deathAndGlory(42));
    }

}
