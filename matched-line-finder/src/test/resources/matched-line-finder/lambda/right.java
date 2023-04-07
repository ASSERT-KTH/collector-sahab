import java.util.function.Function;

class Lambda {
    public String printString() {
        Function<String, String> lambda = (String s) -> s + ":)";
        return lambda.apply("Hey");
    }
}
