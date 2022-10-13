package foo.junit;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

class IDoNotFollowConventions {
    @Test
    public void sum() {
        assertEquals(5, 2+2);
    }
}
