package foo;

public class SpecialFloatingPointValue {
    public static Double generateNaN() {
        Double positiveInfinity = Double.POSITIVE_INFINITY;
        Double negativeInfinity = Double.NEGATIVE_INFINITY;
        return positiveInfinity + negativeInfinity;
    }
}
