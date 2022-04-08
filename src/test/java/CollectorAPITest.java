import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;

import com.sun.jdi.AbsentInformationException;
import java.io.File;
import org.junit.jupiter.api.Test;
import se.kth.debug.Collector;
import se.kth.debug.EventProcessor;
import se.kth.debug.struct.result.BreakPointContext;
import se.kth.debug.struct.result.StackFrameContext;

public class CollectorAPITest {
    @Test
    void invoke_nonStaticFieldsOfStaticClassesShouldNotBeCollected()
            throws AbsentInformationException {
        // arrange
        String[] classpath =
                TestHelper.getMavenClasspathFromBuildDirectory(
                        TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
        String[] tests = new String[] {"foo.StaticClassFieldTest::test_doSomething"};
        File classesAndBreakpoints =
                new File("src/test/resources/sample-maven-project/static-class-field.txt");

        // act
        EventProcessor eventProcessor =
                Collector.invoke(classpath, tests, classesAndBreakpoints, 0, 1, 10, false);
        BreakPointContext bp =
                eventProcessor.getBreakpointContexts().stream()
                        .filter(bpc -> bpc.getLineNumber() == 24)
                        .findAny()
                        .get();
        StackFrameContext sf = bp.getStackFrameContexts().get(0);

        // assert
        assertThat(sf.getRuntimeValueCollection(), is(empty()));
    }
}
