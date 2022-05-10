public class Patch2Time11JGenProg2017 {
    public DSTZone buildTailZone(String id) {
        if (iRules.size() == 2) {
            Rule startRule = iRules.get(0);
            Rule endRule = iRules.get(1);
            if (startRule.getToYear() == Integer.MAX_VALUE &&
                    endRule.getToYear() == Integer.MAX_VALUE) {

                // With exactly two infinitely recurring rules left, a
                // simple DSTZone can be formed.

                // The order of rules can come in any order, and it doesn't
                // really matter which rule was chosen the 'start' and
                // which is chosen the 'end'. DSTZone works properly either
                // way.
                return new DSTZone(id, iStandardOffset,
                        startRule.iRecurrence, endRule.iRecurrence);
            }
        }
        return null;
    }
}
