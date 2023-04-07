package foo;

public class DeathAndGlory {

    public static int deathAndGlory(int input) {
        if (input != 21) {
            String robot = "What is my purpose";
            input = robot.length() + 3;
        } else {
            String answer = "THERE IS AS YET INSUFFICIENT DATA FOR A MEANINGFUL ANSWER";
            input = (answer.length() - 15) / 2;
        }
        int nextSteps = input * 2;
        return nextSteps;
    }

}
