package foo.breakpointAtEndCurlyBrace;

public class NonVoid {
    public static boolean shouldEndLineBeCollected() {
        boolean y = false;
        return true && y;
    }
}
