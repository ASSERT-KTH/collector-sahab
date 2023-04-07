import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ArgumentTest {
    @Test
    void primitive_argumentsValueShouldChange() {
        assertEquals(4, Primitive.add(2,2));
    }

    @Test
    void nonPrimitive_argumentsValueShouldChange() {
        Fraction f1 = new Fraction(1, 1);
        Fraction f2 = new Fraction(1, 4);
        Fraction result = NonPrimitive.add(f1, f2);
        System.out.println(f1);
        System.out.println(f2);
        assertEquals(new Fraction(5, 4), result);
    }
}