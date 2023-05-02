import org.junit.jupiter.api.Test;

import bar.MultipleConstructor;


public class MultipleConstructorTest {

    @Test
    public void testMultipleConstructor() {
        MultipleConstructor multipleConstructor = new MultipleConstructor();
        MultipleConstructor multipleConstructor2 = new MultipleConstructor("foo");
        System.out.println("Called both constructors");
    }
}
