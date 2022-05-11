package foo;

public class AnonymousClass {
    public void implementAnonymousGreetings() {
        Greeting hindiGreeting = new Greeting() {
            @Override
            public void greet() {
                String greeting = "नमस्ते";
                System.out.println(greeting);
            }
        }
        hindiGreeting.greet();
    }
}

interface Greeting {
    void greet();
}
