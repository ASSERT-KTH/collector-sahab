package synthetic;

public class SyntheticMethod {
    public int x;
    public int y;

    public int getX() {
        int result = new InnerClass().z;
        return result;
    }

    private class InnerClass {
        private int z;

        private InnerClass() {
            z = 42;
        }
    }
}