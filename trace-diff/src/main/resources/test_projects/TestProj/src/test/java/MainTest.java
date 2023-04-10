import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class MainTest {

    @Test
    public void test_main(){
        String ret = NumberAnalyzer.analyze(1);
        assertThat(ret, containsString("smaller"));
    }
}
