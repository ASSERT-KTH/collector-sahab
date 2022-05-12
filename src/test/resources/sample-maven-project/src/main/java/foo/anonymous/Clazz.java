package foo.anonymous;

public class Clazz {
    public static void implementAnonymousGreetings() {
        Greeting hindiGreeting = new Greeting() {
            @Override
            public void greet() {
                String greeting = "Namaste";
                System.out.println(greeting);
            }
        };
        hindiGreeting.greet();

        Greeting swedishGreeting = new Greeting() {
            @Override
            public void greet() {
                String greeting = "Tjenare!";
                System.out.println(greeting);
            }
        };
        swedishGreeting.greet();
    }
}

interface Greeting {
    void greet();
}
