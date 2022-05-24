package foo;

public class SwitchCase {
    public static boolean isOdd(int n) {
        switch (n) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                return isOdd(n - 2);
        }
    }
}
