public class NestedAnonymousClass {
    public class TopLevelAnonymous { }
    public class Anonymous1 { }
    public class Anonymous2 { }

    public String makeCodeUnreadable() {
        TopLevelAnonymous tla = new TopLevelAnonymous() {
            @Override
            public String toString() {
                Anonymous1 anonymous1 = new Anonymous1() {
                    @Override
                    public String toString() {
                        Anonymous2 anonymous2 = new Anonymous2() {
                            @Override
                            public String toString() {
                                return "give me some sunshine";
                            }
                        }
                        return anonymous2.toString();
                    }
                }
                return anonymous1.toString();
            }
        }
        return tla.toString();
    }
}
