package foo.anonymous;

import java.util.function.Function;

public class Lambda {
    public static String printString() {
        Function<String, String> lambda = (String s) -> s + "!";
        return lambda.apply("Hey");
    }
}
