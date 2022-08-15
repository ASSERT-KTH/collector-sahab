package foo;

public class SpecialFloatingPointValue {
    public static Double generateNaN() {
        Double positiveInfinity = Double.POSITIVE_INFINITY;
        Double negativeInfinity = Double.NEGATIVE_INFINITY;
        return positiveInfinity + negativeInfinity;
    }

    public static Float generateNaNFromFloat(Float x, Float y) {
        return x + y;
    }

    public static Double[][] giveMeNan() {
        return new Double[][]{{Double.NaN}};
    }
}
