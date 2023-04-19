package foo;

import org.joda.time.chrono.ISOChronology;

// Jackson cannot serialise an instance of `ISOChronology` without a certain module.
// The problem was something else though. We needed to clone the array before storing
// it as runtime value.
public class NonPrimitive {
    public static void setISOChronologyArray() {
        ISOChronology[] chronologies = new ISOChronology[4];
        chronologies[0] = ISOChronology.getInstanceUTC();
        return;
    }
}
