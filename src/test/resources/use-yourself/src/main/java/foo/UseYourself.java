package foo;

public class UseYourself {

    public int state;

    public UseYourself use(UseYourself param) {
        UseYourself localVar = new UseYourself();
        param.state = 42;
        return this;
    }

}
