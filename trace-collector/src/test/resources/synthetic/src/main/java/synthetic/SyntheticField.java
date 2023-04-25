package synthetic;

public class SyntheticField {
    int x;
    int y;

    public class InnerClass {
        int z;

        public InnerClass() {
            z = 42;
        }

        public int getZ() {
            return z;
        }
    }
}
