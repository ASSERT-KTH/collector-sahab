public class NonStaticField {
    public boolean doSomething() {
        Pair p = Pair.createPair(1, 2);
        p.sayHelloWorld();
        return true;
    }

    static class Pair {
        private int a;
        private int b;

        public static Pair createPair(int a, int b) {
            return new Pair(a, b);
        }

        private Pair(int a, int b) {
            this.a = a;
            this.b = b;
        }

        private static void sayHelloWorld() {
            System.out.println("hello world");
            return;
        }
    }
}
