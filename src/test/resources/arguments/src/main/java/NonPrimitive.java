public class NonPrimitive {
    public static Fraction add(Fraction a, Fraction b) {
        a.numerator = a.numerator + a.denominator; // adding 1 to a
        b.numerator = b.numerator - b.denominator; // subtracting 1 from b
        return a.add(b);
    }
}

class Fraction {
    int numerator;
    int denominator;

    Fraction(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    Fraction add(Fraction other) {
        int newNumerator = this.numerator * other.denominator + other.numerator * this.denominator;
        int newDenominator = this.denominator * other.denominator;
        return new Fraction(newNumerator, newDenominator);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof Fraction)) {
            return false;
        }
        Fraction otherFraction = (Fraction) other;
        return this.numerator == otherFraction.numerator && this.denominator == otherFraction.denominator;
    }

    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }
}
