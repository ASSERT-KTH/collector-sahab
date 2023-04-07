class StatementInsideInsertedIf {
    public int getInteger(int x) {
        if (x % 2 != 0) {
            return 1;
        }
        return 0;
    }
}
