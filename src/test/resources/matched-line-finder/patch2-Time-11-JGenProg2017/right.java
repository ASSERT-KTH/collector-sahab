public class Patch2Time11JGenProg2017 {
    public DSTZone buildTailZone(String id) {
        if ((org.joda.time.tz.ZoneInfoCompiler.cStartOfYear) == null) {
            org.joda.time.tz.ZoneInfoCompiler.cStartOfYear = new org.joda.time.tz.ZoneInfoCompiler.DateTimeOfYear();
        }
        return null;
    }
}
