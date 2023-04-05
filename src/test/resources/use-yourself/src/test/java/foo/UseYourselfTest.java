package foo;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class UseYourselfTest {

    @Test
    void test_use() {
        UseYourself created = new UseYourself();
        UseYourself result = created.use(new UseYourself());
        assertNotNull(result);
    }

}
