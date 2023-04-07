class DiffInCase {
    public void doSomething(String s) {
        switch (s) {
            case "s":
                System.out.println("s");
                break;
            case "a":
                System.out.println("a");
                System.out.println("b");
                System.out.println("a");
                break;
            default:
                break;
        }
    }
}
