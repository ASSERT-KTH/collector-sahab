public class NumberAnalyzer {
    public static String analyze(int x){
        if (x < 0)
            return x + " is smaller than zero.";
        else if (x == 0)
            return x + " is exactly zero.";
        else
            return x + " is greater than zero.";
    }
}
