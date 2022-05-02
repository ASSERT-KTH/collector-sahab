package foo.objects;

public class MultipleLevelNestedObject {
    public static int meaningOfLife() {
        LevelA a = new LevelA();
        return a.z.y.x;
    }
}

class LevelA {
    final LevelB z = new LevelB();
}

class LevelB {
    final LevelC y = new LevelC();
}

class LevelC {
    final int x = 42;
}
