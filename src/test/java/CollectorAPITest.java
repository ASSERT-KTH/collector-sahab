import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeFalse;

import com.sun.jdi.AbsentInformationException;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.kth.debug.Collector;
import se.kth.debug.CollectorOptions;
import se.kth.debug.EventProcessor;
import se.kth.debug.struct.result.BreakPointContext;
import se.kth.debug.struct.result.RuntimeValue;
import se.kth.debug.struct.result.RuntimeValueKind;
import se.kth.debug.struct.result.StackFrameContext;

public class CollectorAPITest {

    private static CollectorOptions getDefaultOptions() {
        CollectorOptions context = new CollectorOptions();
        context.setObjectDepth(0);
        context.setStackTraceDepth(1);
        context.setNumberOfArrayElements(10);
        context.setArrayDepth(4);
        context.setSkipPrintingField(false);
        return context;
    }

    @Test
    void invoke_nonStaticFieldsOfStaticClassesShouldNotBeCollected()
            throws AbsentInformationException {
        // arrange
        String[] classpath =
                TestHelper.getMavenClasspathFromBuildDirectory(
                        TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
        String[] tests = new String[] {"foo.StaticClassFieldTest::test_doSomething"};
        File classesAndBreakpoints =
                TestHelper.PATH_TO_INPUT.resolve("static-class-field.txt").toFile();

        // act
        EventProcessor eventProcessor =
                Collector.invoke(classpath, tests, classesAndBreakpoints, getDefaultOptions());
        BreakPointContext bp =
                eventProcessor.getBreakpointContexts().stream()
                        .filter(bpc -> bpc.getLineNumber() == 24)
                        .findAny()
                        .orElseThrow();
        StackFrameContext sf = bp.getStackFrameContexts().get(0);

        // assert
        assertThat(sf.getRuntimeValueCollection(), is(empty()));
    }

    @Nested
    class RepresentingCollections {
        @Test
        void invoke_collectionsAreConvertedToArray() throws AbsentInformationException {
            // arrange
            String[] classpath =
                    TestHelper.getMavenClasspathFromBuildDirectory(
                            TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
            String[] tests = new String[] {"foo.CollectionsTest::test_returnTruthy"};
            File classesAndBreakpoints =
                    TestHelper.PATH_TO_INPUT
                            .resolve("collections")
                            .resolve("one-level.txt")
                            .toFile();

            // act
            EventProcessor eventProcessor =
                    Collector.invoke(classpath, tests, classesAndBreakpoints, getDefaultOptions());

            BreakPointContext breakpoint = eventProcessor.getBreakpointContexts().get(0);
            StackFrameContext stackFrameContext = breakpoint.getStackFrameContexts().get(0);
            List<RuntimeValue> runtimeValues = stackFrameContext.getRuntimeValueCollection();

            // assert
            RuntimeValue queue = runtimeValues.get(0);
            assertThat(queue.getKind(), is(RuntimeValueKind.LOCAL_VARIABLE));
            assertThat(
                    queue.getValueWrapper().getAtomicValue(), equalTo(List.of("Added at runtime")));

            RuntimeValue list = runtimeValues.get(1);
            assertThat(list.getKind(), is(RuntimeValueKind.FIELD));
            assertThat(list.getValueWrapper().getAtomicValue(), equalTo(List.of(1, 2, 3, 4, 5)));

            RuntimeValue set = runtimeValues.get(2);
            assertThat(set.getKind(), is(RuntimeValueKind.FIELD));
            assertThat(
                    ((List<String>) set.getValueWrapper().getAtomicValue()),
                    containsInAnyOrder("aman", "sahab", "sharma"));
        }

        @Test
        void invoke_nestedCollectionsAreRepresentedCorrectly() throws AbsentInformationException {
            // arrange
            String[] classpath =
                    TestHelper.getMavenClasspathFromBuildDirectory(
                            TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
            String[] tests = new String[] {"foo.CollectionsTest::test_returnFalsy"};
            File classesAndBreakpoints =
                    TestHelper.PATH_TO_INPUT.resolve("collections").resolve("nested.txt").toFile();

            // act
            EventProcessor eventProcessor =
                    Collector.invoke(classpath, tests, classesAndBreakpoints, getDefaultOptions());

            BreakPointContext breakpoint = eventProcessor.getBreakpointContexts().get(0);
            StackFrameContext stackFrameContext = breakpoint.getStackFrameContexts().get(0);
            List<RuntimeValue> runtimeValues = stackFrameContext.getRuntimeValueCollection();

            // assert
            assumeFalse(true);
        }
    }
}
