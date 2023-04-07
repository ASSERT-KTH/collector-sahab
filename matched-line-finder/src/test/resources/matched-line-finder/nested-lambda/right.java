public class NestedLambda {
    public int iHaveNestedLambda() {
        Function1 f1 = () -> {
            Function2 f2 = () -> {
                return 420;
            };
            return f2;
        };
        return f1.lambda1().lambda2();
    }
}

interface Function1 {
    Function2 lambda1();
}

interface Function2 {
    int lambda2();
}
