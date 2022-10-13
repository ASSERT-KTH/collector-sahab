package foo.junit;

public abstract class AbstractTest {
    @org.junit.Test
    public void test_junit4() {
        assert "foo".concat("bar").equals("foobar");
    }
}

abstract class AbstractTestForJUnit5 {
    @org.junit.jupiter.api.Test
    public void test_junit5() {
        assert "foo".concat("bar").equals("foobar");
    }
}
