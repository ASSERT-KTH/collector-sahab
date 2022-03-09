package junitTests;

import org.junit.Test

public class SomeAreJunitTest {
    @Test
    public void should_we_exist() {
        assert 0 == 1;
    }

    @org.junit.jupiter.api.Test
    void doesLifeHaveMeaning() {
        assert 42 == 42;
    }

    void pleaseIgnoreMe() { }
}
