package se.assertkth.tracediff.models;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class SourceInfoTest {

    @Test
    void computeLineVars_simple(){
        File src = Paths.get("src/test/resources/source/simple/DateTimeZoneBuilder.java").toFile();
        SourceInfo slv = new SourceInfo(src);
        assertEquals(slv.getLineVars().get(1029).size(), 2);
        assertTrue(slv.getLineVars().get(1029).contains("millis"));
    }
}
