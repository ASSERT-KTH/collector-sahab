public class MultipleTypes {
    public static void doNothing() {
        int x = 24;
        System.out.println("I am printing");
    }

    class FirstLevel1 {
        public void whyDoIWriteTests() {
            String l = "never gonna give you up!"
                    + "never gonna let you down"
                    + "never gonna turn around"
                    + "and desert you";
            System.out.println("it makes me feel secure");
        }
    }

    class FirstLevel2 {
        class SecondLevel1 {
            public int getWomansAgeOnTinder() {
                int age = 20;
                return age - 2;
            }
        }
    }
}
