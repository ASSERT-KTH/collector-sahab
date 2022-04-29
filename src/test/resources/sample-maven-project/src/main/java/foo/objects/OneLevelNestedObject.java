package foo.objects;

public class OneLevelNestedObject {
    public Shape getATriangle() {
        return new Triangle();
    }
}

class Shape {
    int sides;
}

class Triangle extends Shape {
    Triangle() {
        this.sides = 3;
    }
}
