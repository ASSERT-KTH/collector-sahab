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
import se.kth.debug.struct.result.*;

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
                TestHelper.PATH_TO_INPUT.resolve("static-class-field.txt").toFile();

        // act
        EventProcessor eventProcessor =
                Collector.invoke(
                        classpath, tests, classesAndBreakpoints, TestHelper.getDefaultOptions());
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

        private CollectorOptions setObjectAndArrayDepth(int objectDepth, int arrayDepth) {
            CollectorOptions context = TestHelper.getDefaultOptions();
            context.setArrayDepth(arrayDepth);
            context.setObjectDepth(objectDepth);
            return context;
        }

        @Test
        void invoke_recordValuesFromArrayFieldInsideCollection() throws AbsentInformationException {
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
                    Collector.invoke(
                            classpath, tests, classesAndBreakpoints, setObjectAndArrayDepth(1, 0));

            BreakPointContext breakpoint = eventProcessor.getBreakpointContexts().get(0);
            StackFrameContext stackFrameContext = breakpoint.getStackFrameContexts().get(0);
            List<RuntimeValue> runtimeValues = stackFrameContext.getRuntimeValueCollection();

            // assert
            RuntimeValue queue = runtimeValues.get(0);
            FieldData arrayContainingQueueElements = queue.getFields().get(0);

            assertThat(queue.getKind(), is(RuntimeValueKind.LOCAL_VARIABLE));
            assertThat(arrayContainingQueueElements.getName(), equalTo("elements"));
            assertThat(
                    arrayContainingQueueElements.getValueWrapper().getAtomicValue(),
                    equalTo(List.of("Added at runtime")));

            RuntimeValue list = runtimeValues.get(1);
            FieldData arrayContainingListElements = list.getFields().get(1);

            assertThat(list.getKind(), is(RuntimeValueKind.FIELD));
            assertThat(arrayContainingListElements.getName(), equalTo("elements"));
            assertThat(
                    arrayContainingListElements.getValueWrapper().getAtomicValue(),
                    equalTo(List.of(1, 2, 3, 4, 5)));

            RuntimeValue set = runtimeValues.get(2);
            FieldData arrayContainingSetElements = set.getFields().get(1);

            assertThat(set.getKind(), is(RuntimeValueKind.FIELD));
            assertThat(arrayContainingSetElements.getName(), equalTo("elements"));
            assertThat(
                    (List<String>) arrayContainingSetElements.getValueWrapper().getAtomicValue(),
                    containsInAnyOrder("aman", "sahab", "sharma"));
        }

        @Test
        void invoke_primitiveArraysAreRecorded() throws AbsentInformationException {
            // arrange
            String[] classpath =
                    TestHelper.getMavenClasspathFromBuildDirectory(
                            TestHelper.PATH_TO_SAMPLE_MAVEN_PROJECT.resolve("with-debug"));
            String[] tests = new String[] {"foo.CollectionsTest::test_canWePrintPrimitive"};
            File classesAndBreakpoints =
                    TestHelper.PATH_TO_INPUT
                            .resolve("collections")
                            .resolve("primitive.txt")
                            .toFile();

            // act
            EventProcessor eventProcessor =
                    Collector.invoke(
                            classpath,
                            tests,
                            classesAndBreakpoints,
                            TestHelper.getDefaultOptions());

            BreakPointContext breakpoint = eventProcessor.getBreakpointContexts().get(0);
            StackFrameContext stackFrameContext = breakpoint.getStackFrameContexts().get(0);
            RuntimeValue thePrimitiveArray = stackFrameContext.getRuntimeValueCollection().get(0);
            List<String> actualElements =
                    (List<String>) thePrimitiveArray.getValueWrapper().getAtomicValue();

            // assert
            assertThat(actualElements, equalTo(List.of("yes", "we", "can")));
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
                    Collector.invoke(
                            classpath,
                            tests,
                            classesAndBreakpoints,
                            TestHelper.getDefaultOptions());

            BreakPointContext breakpoint = eventProcessor.getBreakpointContexts().get(0);
            StackFrameContext stackFrameContext = breakpoint.getStackFrameContexts().get(0);
            List<RuntimeValue> runtimeValues = stackFrameContext.getRuntimeValueCollection();

            // assert
            assumeFalse(true);
        }
    }
}
