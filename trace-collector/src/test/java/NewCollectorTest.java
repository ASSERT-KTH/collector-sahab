import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static se.assertkth.collector.util.JavaAgentPath.getAgentPath;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.assertkth.cs.commons.runtime.LineSnapshot;
import se.assertkth.cs.commons.runtime.RuntimeReturnedValue;
import se.assertkth.cs.commons.runtime.RuntimeValue;
import se.assertkth.cs.commons.runtime.RuntimeValue.Kind;
import se.assertkth.cs.commons.runtime.StackFrameContext;
import se.assertkth.cs.commons.runtime.output.SahabOutput;

class NewCollectorTest {
    @Test
    void basicMath_test() throws MavenInvocationException, IOException {
        // act
        InvocationResult result = getInvocationResult(new File("src/test/resources/basic-math/pom.xml"), List.of(), "");

        // assert
        assertThat(result.getExitCode(), equalTo(1));
        File actualOutput = new File("src/test/resources/basic-math/target/output.json");
        assertThat(actualOutput.exists(), equalTo(true));

        ObjectMapper mapper = new ObjectMapper();
        SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
        assertThat(output.getBreakpoint().size(), equalTo(2));

        LineSnapshot snapshot0 = output.getBreakpoint().get(0);
        LineSnapshot snapshot1 = output.getBreakpoint().get(1);
        assertThat(snapshot0.getLineNumber(), equalTo(5));
        assertThat(snapshot1.getLineNumber(), equalTo(9));

        // StackFrameContext
        StackFrameContext theOnlyStackFrameContext =
                snapshot0.getStackFrameContext().get(0);
        List<RuntimeValue> runtimeValues = theOnlyStackFrameContext.getRuntimeValueCollection();
        RuntimeValue runtimeValue0 = runtimeValues.get(0);
        assertThat(runtimeValue0.getValue(), equalTo(23));
        assertThat(runtimeValue0.getType(), equalTo(int.class.getName()));

        RuntimeValue runtimeValue1 = runtimeValues.get(1);
        assertThat(runtimeValue1.getValue(), equalTo(2));
        assertThat(runtimeValue1.getType(), equalTo(int.class.getName()));

        // Returned value
        assertThat(output.getReturns().size(), equalTo(1));
        RuntimeReturnedValue returnedValue = output.getReturns().get(0);

        assertThat(returnedValue.getValue(), equalTo(25));
    }

    @Test
    @DisplayName("Dead variables are removed from later line logs")
    void deadVariablesShouldBeRemoved() throws MavenInvocationException, IOException {
        InvocationResult result = getInvocationResult(
                new File("src/test/resources/liveness-and-death/pom.xml"),
                List.of(
                        "classesAndBreakpoints=src/test/resources/death-and-glory.txt",
                        "output=target/death-and-glory.json",
                        "executionDepth=1"),
                "");

        assertThat(result.getExitCode(), equalTo(0));
        File actualOutput = new File("src/test/resources/liveness-and-death/target/death-and-glory.json");
        assertThat(actualOutput.exists(), equalTo(true));

        ObjectMapper mapper = new ObjectMapper();
        SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
        assertThat(output.getBreakpoint().size(), equalTo(4));

        LineSnapshot snapshot8 = output.getBreakpoint().get(2);
        LineSnapshot snapshot11 = output.getBreakpoint().get(0);
        LineSnapshot snapshot14 = output.getBreakpoint().get(1);

        List<RuntimeValue> values8 = snapshot8.getStackFrameContext().get(0).getRuntimeValueCollection();
        assertThat(
                values8.get(0),
                equalTo(RuntimeValue.fromRaw(Kind.LOCAL_VARIABLE, "input", "int", 42, List.of(), List.of())));
        assertThat(
                values8.get(1),
                equalTo(RuntimeValue.fromRaw(
                        Kind.LOCAL_VARIABLE, "robot", "java.lang.String", "What is my purpose", List.of(), List.of())));

        List<RuntimeValue> values11 = snapshot11.getStackFrameContext().get(0).getRuntimeValueCollection();
        assertThat(
                values11.get(0),
                equalTo(RuntimeValue.fromRaw(Kind.LOCAL_VARIABLE, "input", "int", 21, List.of(), List.of())));
        assertThat(
                values11.get(1),
                equalTo(RuntimeValue.fromRaw(
                        Kind.LOCAL_VARIABLE,
                        "answer",
                        "java.lang.String",
                        "THERE IS AS YET INSUFFICIENT DATA FOR A MEANINGFUL ANSWER",
                        List.of(),
                        List.of())));

        List<RuntimeValue> values14 = snapshot14.getStackFrameContext().get(0).getRuntimeValueCollection();
        assertThat(
                values14.get(0),
                equalTo(RuntimeValue.fromRaw(Kind.LOCAL_VARIABLE, "input", "int", 21, List.of(), List.of())));
        assertThat(
                values14.get(1),
                equalTo(RuntimeValue.fromRaw(Kind.LOCAL_VARIABLE, "nextSteps", "int", 42, List.of(), List.of())));
    }

    @Nested
    class Cycles {

        @Test
        @DisplayName("Using your own class as a local variable should work")
        void deadVariablesShouldBeRemoved() throws MavenInvocationException, IOException {
            InvocationResult result = getInvocationResult(
                    new File("src/test/resources/use-yourself/pom.xml"),
                    List.of(
                            "classesAndBreakpoints=src/test/resources/use-yourself.txt",
                            "methodsForExitEvent=src/test/resources/use-yourself.json",
                            "output=target/use-yourself.json",
                            "executionDepth=1"),
                    "");

            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/use-yourself/target/use-yourself.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assertThat(output.getBreakpoint().size(), equalTo(1));
            assertThat(output.getReturns().size(), equalTo(1));

            List<RuntimeValue> values =
                    output.getBreakpoint().get(0).getStackFrameContext().get(0).getRuntimeValueCollection();
            assertThat(
                    values.get(0),
                    equalTo(RuntimeValue.fromRaw(
                            Kind.LOCAL_VARIABLE,
                            "param",
                            "foo.UseYourself",
                            "foo.UseYourself",
                            List.of(RuntimeValue.fromRaw(
                                    // has updated value
                                    Kind.FIELD, "state", "int", 42, List.of(), List.of())),
                            List.of())));
            assertThat(
                    values.get(1),
                    equalTo(RuntimeValue.fromRaw(
                            Kind.LOCAL_VARIABLE,
                            "localVar",
                            "foo.UseYourself",
                            "foo.UseYourself",
                            List.of(RuntimeValue.fromRaw(Kind.FIELD, "state", "int", 0, List.of(), List.of())),
                            List.of())));
            assertThat(
                    values.get(2), equalTo(RuntimeValue.fromRaw(Kind.FIELD, "state", "int", 0, List.of(), List.of())));

            RuntimeReturnedValue returnedValue = output.getReturns().get(0);
            assertThat(returnedValue.getKind(), equalTo(Kind.RETURN));
            assertThat(returnedValue.getValue(), equalTo("foo.UseYourself"));
            assertThat(returnedValue.getType(), equalTo("foo.UseYourself"));
            // Original, unchanged value
            assertThat(
                    returnedValue.getFields(),
                    equalTo(List.of(RuntimeValue.fromRaw(Kind.FIELD, "state", "int", 0, List.of(), List.of()))));
        }
    }

    @Nested
    class SpecialFloatingPointValues {
        @Test
        void shouldBeAbleToSerialise_specialFloatingPointValue() throws MavenInvocationException, IOException {
            // act
            InvocationResult result = getInvocationResult(
                    new File("src/test/resources/special-floating-point-value/pom.xml"),
                    List.of("classesAndBreakpoints=src/test/resources/linear.txt", "output=target/linear.json"),
                    "");

            // assert
            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/special-floating-point-value/target/linear.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assertThat(output.getBreakpoint().size(), equalTo(4));

            LineSnapshot snapshot0 = output.getBreakpoint().get(0);
            LineSnapshot snapshot1 = output.getBreakpoint().get(1);
            LineSnapshot snapshot2 = output.getBreakpoint().get(2);
            LineSnapshot snapshot3 = output.getBreakpoint().get(3);

            // Line 11
            assertThat(snapshot0.getLineNumber(), equalTo(11));
            StackFrameContext theOnlyStackFrameContext =
                    snapshot0.getStackFrameContext().get(0);
            assertThat(theOnlyStackFrameContext.getRuntimeValueCollection().size(), equalTo(2));
            RuntimeValue runtimeValue0_11 =
                    theOnlyStackFrameContext.getRuntimeValueCollection().get(0);
            assertThat(runtimeValue0_11.getName(), equalTo("x"));
            assertThat(runtimeValue0_11.getValue(), equalTo(Objects.toString(Float.POSITIVE_INFINITY)));
            assertThat(runtimeValue0_11.getType(), equalTo(Float.class.getName()));

            // Line 5
            assertThat(snapshot1.getLineNumber(), equalTo(5));
            theOnlyStackFrameContext = snapshot1.getStackFrameContext().get(0);
            assertThat(theOnlyStackFrameContext.getRuntimeValueCollection(), is(empty()));

            // Line 6
            assertThat(snapshot2.getLineNumber(), equalTo(6));
            theOnlyStackFrameContext = snapshot2.getStackFrameContext().get(0);
            assertThat(theOnlyStackFrameContext.getRuntimeValueCollection().size(), equalTo(1));
            RuntimeValue runtimeValue0_6 =
                    theOnlyStackFrameContext.getRuntimeValueCollection().get(0);
            assertThat(runtimeValue0_6.getName(), equalTo("positiveInfinity"));
            assertThat(runtimeValue0_6.getValue(), equalTo(Objects.toString(Double.POSITIVE_INFINITY)));
            assertThat(runtimeValue0_6.getType(), equalTo(Double.class.getName()));

            // Line 7
            assertThat(snapshot3.getLineNumber(), equalTo(7));
            theOnlyStackFrameContext = snapshot3.getStackFrameContext().get(0);
            assertThat(theOnlyStackFrameContext.getRuntimeValueCollection().size(), equalTo(2));
            RuntimeValue runtimeValue1 =
                    theOnlyStackFrameContext.getRuntimeValueCollection().get(1);
            assertThat(runtimeValue1.getName(), equalTo("negativeInfinity"));
            assertThat(runtimeValue1.getValue(), equalTo(Objects.toString(Double.NEGATIVE_INFINITY)));
            assertThat(runtimeValue1.getType(), equalTo(Double.class.getName()));
        }

        @Test
        void shouldBeAbleToSerialise_nestedSpecialFloatingPointValue() throws MavenInvocationException, IOException {
            // act
            InvocationResult result = getInvocationResult(
                    new File("src/test/resources/special-floating-point-value/pom.xml"),
                    List.of(
                            "methodsForExitEvent=src/test/resources/nested.json",
                            "classesAndBreakpoints=src/test/resources/nested.txt",
                            "output=target/nested.json"),
                    "");

            // assert
            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/special-floating-point-value/target/nested.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assertThat(output.getReturns().size(), equalTo(1));
        }
    }

    @Nested
    class RepresentingCollections {
        @Test
        void invoke_recordValuesFromArrayFieldInsideCollection() throws MavenInvocationException, IOException {
            // arrange
            File pomFile = new File("src/test/resources/collections/pom.xml");
            InvocationResult result = getInvocationResult(
                    pomFile,
                    List.of(
                            "classesAndBreakpoints=src/test/resources/one-level.txt",
                            "output=target/one-level-collection.json",
                            "executionDepth=1"),
                    "-Dtest=foo.CollectionsTest#test_returnTruthy");

            // assert
            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/collections/target/one-level-collection.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assertThat(output.getBreakpoint().size(), equalTo(1));

            StackFrameContext theOnlyStackTraceContext =
                    output.getBreakpoint().get(0).getStackFrameContext().get(0);
            RuntimeValue arrayDequeue =
                    theOnlyStackTraceContext.getRuntimeValueCollection().get(0);
            assertThat(arrayDequeue.getName(), equalTo("q"));

            // elementsOfQueue ("elements") is an array inside the ArrayDeque
            RuntimeValue elementsOfQueue = arrayDequeue.getFields().get(0);
            assertThat(elementsOfQueue.getName(), equalTo("elements"));

            String firstElement = (String) ((List<?>) elementsOfQueue.getValue()).get(0);
            assertThat(firstElement, equalTo("Added at runtime"));

            // static fields
            // list
            RuntimeValue list =
                    theOnlyStackTraceContext.getRuntimeValueCollection().get(1);
            assertThat(list.getName(), equalTo("list"));
            RuntimeValue elementsOfList = list.getFields().get(1);

            List<?> atomicValue = (List<?>) elementsOfList.getValue();
            assertThat(atomicValue, equalTo(List.of(1, 2, 3, 4, 5)));

            // set
            RuntimeValue set =
                    theOnlyStackTraceContext.getRuntimeValueCollection().get(2);
            assertThat(set.getName(), equalTo("set"));
            RuntimeValue elementsOfSet = set.getFields().get(1);

            assertThat(elementsOfSet.getName(), equalTo("elements"));
            // null are pre-allocated buffers in HashSet
            assertThat(
                    (List<?>) elementsOfSet.getValue(),
                    containsInAnyOrder("aman", "sharma", "sahab", null, null, null));
        }

        @Test
        void invoke_primitiveArraysAreRecorded() throws MavenInvocationException, IOException {
            File pomFile = new File("src/test/resources/collections/pom.xml");
            InvocationResult result = getInvocationResult(
                    pomFile,
                    List.of(
                            "classesAndBreakpoints=src/test/resources/primitive.txt",
                            "output=target/primitive.json",
                            "executionDepth=1"),
                    "-Dtest=foo.CollectionsTest#test_canWePrintPrimitive");

            // assert
            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/collections/target/primitive.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assertThat(output.getBreakpoint().size(), equalTo(1));

            List<RuntimeValue> runtimeValues =
                    output.getBreakpoint().get(0).getStackFrameContext().get(0).getRuntimeValueCollection();

            List<?> stringsInsideArray = (List<?>) runtimeValues.get(0).getValue();
            assertThat(stringsInsideArray, equalTo(List.of("yes", "we", "can")));
        }

        @Nested
        class NestedArraysAreRepresentedCorrectly {

            private RuntimeValue getStackFrameContext(int depth) throws MavenInvocationException, IOException {
                File pomFile = new File("src/test/resources/collections/pom.xml");
                InvocationResult result = getInvocationResult(
                        pomFile,
                        List.of(
                                "classesAndBreakpoints=src/test/resources/nested-array.txt",
                                "output=target/nested-array.json",
                                "executionDepth=" + depth),
                        "-Dtest=foo.CollectionsTest#test_canNestedArrayBeRepresented");

                assertThat(result.getExitCode(), equalTo(0));
                File actualOutput = new File("src/test/resources/collections/target/nested-array.json");
                assertThat(actualOutput.exists(), equalTo(true));

                ObjectMapper mapper = new ObjectMapper();
                SahabOutput sahabOutput = mapper.readValue(actualOutput, new TypeReference<>() {});

                return sahabOutput
                        .getBreakpoint()
                        .get(0)
                        .getStackFrameContext()
                        .get(0)
                        .getRuntimeValueCollection()
                        .get(0);
            }

            @Test
            void nestedArray_depth0() throws MavenInvocationException, IOException {
                // arrange
                RuntimeValue nestedArray = getStackFrameContext(0);
                assertThat(nestedArray.getName(), equalTo("x"));
                assertThat(nestedArray.getType(), equalTo("int[][][]"));
                assertThat(nestedArray.getArrayElements(), is(empty()));
            }

            @Test
            void nestedArray_depth1() throws MavenInvocationException, IOException {
                // arrange
                RuntimeValue nestedArray = getStackFrameContext(1);
                assertThat(nestedArray.getArrayElements().size(), equalTo(2));

                RuntimeValue array0 = nestedArray.getArrayElements().get(0);
                assertThat(array0.getKind(), equalTo(RuntimeValue.Kind.ARRAY_ELEMENT));
                assertThat(array0.getType(), equalTo("int[][]"));
                assertThat(array0.getArrayElements(), is(empty()));

                RuntimeValue array1 = nestedArray.getArrayElements().get(1);
                assertThat(array1.getKind(), equalTo(RuntimeValue.Kind.ARRAY_ELEMENT));
                assertThat(array1.getType(), equalTo("int[][]"));
                assertThat(array1.getArrayElements(), is(empty()));
            }

            @Test
            void nestedArray_depth2() throws MavenInvocationException, IOException {
                // arrange
                RuntimeValue nestedArray = getStackFrameContext(2);

                // assert
                RuntimeValue array0 =
                        nestedArray.getArrayElements().get(0).getArrayElements().get(0);
                List<?> actualValues_0 = (List<?>) array0.getValue();
                assertThat(actualValues_0, equalTo(List.of(1)));

                RuntimeValue array1 =
                        nestedArray.getArrayElements().get(0).getArrayElements().get(1);
                List<?> actualValues_1 = (List<?>) array1.getValue();
                assertThat(actualValues_1, equalTo(List.of(2)));

                RuntimeValue array2 =
                        nestedArray.getArrayElements().get(1).getArrayElements().get(0);
                List<?> actualValues_2 = (List<?>) array2.getValue();
                assertThat(actualValues_2, equalTo(List.of(3, 4, 5)));

                RuntimeValue array3 =
                        nestedArray.getArrayElements().get(1).getArrayElements().get(1);
                List<?> actualValues_3 = (List<?>) array3.getValue();
                assertThat(actualValues_3, equalTo(List.of(5, 3)));
            }

            @Test
            void invoke_elementsInsideNestedSetAreRecorded() throws MavenInvocationException, IOException {
                // arrange
                File pomFile = new File("src/test/resources/collections/pom.xml");
                InvocationResult result = getInvocationResult(
                        pomFile,
                        List.of(
                                "classesAndBreakpoints=src/test/resources/nested-collection.txt",
                                "output=target/nested-collection.json",
                                "executionDepth=8"),
                        "-Dtest=foo.CollectionsTest#test_canWeRepresentNestedCollection");

                // assert
                assertThat(result.getExitCode(), equalTo(0));
                File actualOutput = new File("src/test/resources/collections/target/nested-collection.json");
                assertThat(actualOutput.exists(), equalTo(true));

                ObjectMapper mapper = new ObjectMapper();
                SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
                assertThat(output.getBreakpoint().size(), equalTo(1));

                List<RuntimeValue> runtimeValues = output.getBreakpoint()
                        .get(0)
                        .getStackFrameContext()
                        .get(0)
                        .getRuntimeValueCollection();

                RuntimeValue onlyItemInsideNestedSet = runtimeValues
                        .get(0)
                        .getFields()
                        .get(1)
                        .getFields()
                        .get(7)
                        .getArrayElements()
                        .get(10)
                        .getFields()
                        .get(1)
                        .getFields()
                        .get(1)
                        .getFields()
                        .get(7)
                        .getArrayElements()
                        .get(10)
                        .getFields()
                        .get(1);

                assertThat(onlyItemInsideNestedSet.getValue(), equalTo("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
            }
        }
    }

    @Test
    void arrayOfObjects() throws IOException, MavenInvocationException {
        // act
        File pomFile = new File("src/test/resources/array-of-objects/pom.xml");
        InvocationResult result = getInvocationResult(
                pomFile,
                List.of(
                        "classesAndBreakpoints=src/test/resources/array-of-objects.txt",
                        "output=target/output.json",
                        "executionDepth=1"),
                "-Dtest=ArrayOfObjectsTest");

        // assert
        assertThat(result.getExitCode(), equalTo(0));
        File actualOutput = new File("src/test/resources/array-of-objects/target/output.json");
        assertThat(actualOutput.exists(), equalTo(true));

        ObjectMapper mapper = new ObjectMapper();
        SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
        assertThat(output.getBreakpoint().size(), equalTo(1));

        List<RuntimeValue> arrayElements = output.getBreakpoint()
                .get(0)
                .getStackFrameContext()
                .get(0)
                .getRuntimeValueCollection()
                .get(0)
                .getArrayElements();

        List<String> entries = arrayElements.stream()
                .map(RuntimeValue::getValue)
                .map(Object::toString)
                .collect(Collectors.toList());

        assertThat(entries, equalTo(List.of("CustomObject", "CustomObject")));

        RuntimeValue entry0 = arrayElements.get(0);
        RuntimeValue entry1 = arrayElements.get(1);

        assertThat(entry0.getFields(), is(empty()));
        assertThat(entry1.getFields(), is(empty()));
    }

    @Test
    void nonStaticField_shouldNotBeRecorded() throws IOException, MavenInvocationException {
        // act
        File pomFile = new File("src/test/resources/non-static-field/pom.xml");
        InvocationResult result = getInvocationResult(
                pomFile,
                List.of(
                        "classesAndBreakpoints=src/test/resources/non-static-field.txt",
                        "output=target/output.json",
                        "executionDepth=1"),
                "-Dtest=NonStaticFieldTest");

        // assert
        assertThat(result.getExitCode(), equalTo(0));
        File actualOutput = new File("src/test/resources/non-static-field/target/output.json");
        assertThat(actualOutput.exists(), equalTo(true));

        ObjectMapper mapper = new ObjectMapper();
        SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
        assertThat(output.getBreakpoint(), is(empty()));
        assertThat(output.getReturns(), is(empty()));
    }

    @Test
    void twins_onlyRecordOneMethodExit() throws IOException, MavenInvocationException {
        // act
        File pomFile = new File("src/test/resources/twins/pom.xml");
        InvocationResult result = getInvocationResult(
                pomFile,
                List.of(
                        "methodsForExitEvent=src/test/resources/methods.json",
                        "output=target/output.json",
                        "executionDepth=0"),
                "-Dtest=TwinsTest");

        // assert
        assertThat(result.getExitCode(), equalTo(0));
        File actualOutput = new File("src/test/resources/twins/target/output.json");
        assertThat(actualOutput.exists(), equalTo(true));

        ObjectMapper mapper = new ObjectMapper();
        SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
        assertThat(output.getBreakpoint(), is(empty()));
        assertThat(output.getReturns().size(), equalTo(1));

        RuntimeValue returnValue = output.getReturns().get(0);

        assertThat(returnValue.getName(), equalTo("getValue"));
        assertThat(returnValue.getValue(), equalTo("a"));
        assertThat(returnValue.getType(), equalTo("java.lang.String"));
        assertThat(returnValue.getFields(), is(empty()));
        assertThat(returnValue.getArrayElements(), is(empty()));
    }

    @Nested
    class VoidMethod {
        @Test
        void voidMethod_boxedVoidValueShouldBeRecorded() throws IOException, MavenInvocationException {
            // act
            File pomFile = new File("src/test/resources/void-method/pom.xml");
            InvocationResult result = getInvocationResult(
                    pomFile,
                    List.of(
                            "methodsForExitEvent=src/test/resources/boxed-void.json",
                            "output=target/boxed-void.json",
                            "executionDepth=0"),
                    "-Dtest=VoidMethodTest#test_doNotReturnAnything_boxedVoid");

            // assert
            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/void-method/target/boxed-void.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assertThat(output.getBreakpoint(), is(empty()));
            assertThat(output.getReturns().size(), equalTo(1));

            RuntimeValue returnValue = output.getReturns().get(0);

            assertThat(returnValue.getName(), equalTo("doNotReturnAnything"));
            assertThat(returnValue.getValue(), is(nullValue()));
            assertThat(returnValue.getType(), equalTo("java.lang.Void"));
            assertThat(returnValue.getFields(), is(empty()));
            assertThat(returnValue.getArrayElements(), is(empty()));
        }

        @Test
        void voidMethod_voidValueShouldBeRecorded() throws IOException, MavenInvocationException {
            // act
            File pomFile = new File("src/test/resources/void-method/pom.xml");
            InvocationResult result = getInvocationResult(
                    pomFile,
                    List.of(
                            "methodsForExitEvent=src/test/resources/void.json",
                            "output=target/void.json",
                            "executionDepth=0"),
                    "-Dtest=VoidMethodTest#test_doNotReturnAnything_void");

            // assert
            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/void-method/target/void.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assertThat(output.getBreakpoint(), is(empty()));
            assertThat(output.getReturns().size(), equalTo(1));

            RuntimeValue returnValue = output.getReturns().get(0);

            assertThat(returnValue.getName(), equalTo("doNotReturnAnything"));
            assertThat(returnValue.getValue(), is(nullValue()));
            assertThat(returnValue.getType(), equalTo("void"));
            assertThat(returnValue.getFields(), is(empty()));
            assertThat(returnValue.getArrayElements(), is(empty()));
        }
    }

    @Test
    void anonymousClass_onlyInnerLocalVariableShouldBeRecorded() throws MavenInvocationException, IOException {
        // act
        File pomFile = new File("src/test/resources/anonymous-class/pom.xml");
        InvocationResult result = getInvocationResult(
                pomFile,
                List.of(
                        "classesAndBreakpoints=src/test/resources/input.txt",
                        "output=target/output.json",
                        "executionDepth=0"),
                "-Dtest=AnonymousClassTest");

        // assert
        assertThat(result.getExitCode(), equalTo(0));
        File actualOutput = new File("src/test/resources/anonymous-class/target/output.json");
        assertThat(actualOutput.exists(), equalTo(true));

        ObjectMapper mapper = new ObjectMapper();
        SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
        assertThat(output.getBreakpoint().size(), equalTo(2));
        assertThat(output.getReturns(), is(empty()));

        RuntimeValue hindiGreeting = output.getBreakpoint()
                .get(0)
                .getStackFrameContext()
                .get(0)
                .getRuntimeValueCollection()
                .get(0);
        assertThat(hindiGreeting.getValue(), equalTo("Namaste"));

        RuntimeValue swedishGreeting = output.getBreakpoint()
                .get(1)
                .getStackFrameContext()
                .get(0)
                .getRuntimeValueCollection()
                .get(0);
        assertThat(swedishGreeting.getValue(), equalTo("Tjenare!"));
    }

    @Nested
    class Arguments {
        @Test
        void primitiveArgumentValues_shouldBeRecorded() throws MavenInvocationException, IOException {
            // act
            File pomFile = new File("src/test/resources/arguments/pom.xml");
            InvocationResult result = getInvocationResult(
                    pomFile,
                    List.of(
                            "extractParameters=true",
                            "methodsForExitEvent=src/test/resources/primitive.json",
                            "classesAndBreakpoints=src/test/resources/primitive.txt",
                            "output=target/output.json",
                            "executionDepth=0"),
                    "-Dtest=ArgumentTest#primitive_argumentsValueShouldChange");

            // assert
            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/arguments/target/output.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assertThat(output.getBreakpoint().size(), equalTo(1));
            assertThat(output.getReturns().size(), equalTo(1));

            // breakpoints
            RuntimeValue localVariable1 = output.getBreakpoint()
                    .get(0)
                    .getStackFrameContext()
                    .get(0)
                    .getRuntimeValueCollection()
                    .get(0);
            assertThat(localVariable1.getName(), equalTo("a"));
            assertThat(localVariable1.getValue(), equalTo(3));

            RuntimeValue localVariable2 = output.getBreakpoint()
                    .get(0)
                    .getStackFrameContext()
                    .get(0)
                    .getRuntimeValueCollection()
                    .get(1);
            assertThat(localVariable2.getName(), equalTo("b"));
            assertThat(localVariable2.getValue(), equalTo(1));

            // return
            RuntimeReturnedValue returnValue = output.getReturns().get(0);
            assertThat(returnValue.getArguments().size(), equalTo(2));

            RuntimeValue argument1 = returnValue.getArguments().get(0);
            assertThat(argument1.getName(), equalTo("a"));
            assertThat(argument1.getValue(), equalTo(2));
            assertThat(((int) argument1.getValue()) + 1, equalTo(localVariable1.getValue()));

            RuntimeValue argument2 = returnValue.getArguments().get(1);
            assertThat(argument2.getName(), equalTo("b"));
            assertThat(argument2.getValue(), equalTo(2));
            assertThat(((int) argument2.getValue()) - 1, equalTo(localVariable2.getValue()));
        }

        @Test
        void nonPrimitiveArgumentValues_shouldBeRecorded() throws MavenInvocationException, IOException {
            // act
            File pomFile = new File("src/test/resources/arguments/pom.xml");
            InvocationResult result = getInvocationResult(
                    pomFile,
                    List.of(
                            "extractParameters=true",
                            "methodsForExitEvent=src/test/resources/non-primitive.json",
                            "classesAndBreakpoints=src/test/resources/non-primitive.txt",
                            "output=target/output.json",
                            "executionDepth=1"),
                    "-Dtest=ArgumentTest#nonPrimitive_argumentsValueShouldChange");

            // assert
            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/arguments/target/output.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assertThat(output.getBreakpoint().size(), equalTo(1));
            assertThat(output.getReturns().size(), equalTo(1));

            // return
            RuntimeReturnedValue returnValue = output.getReturns().get(0);
            assertThat(returnValue.getArguments().size(), equalTo(2));

            RuntimeValue argument1 = returnValue.getArguments().get(0);
            assertThat(argument1.getName(), equalTo("a"));

            List<RuntimeValue> a1Fields = argument1.getFields();
            assertThat(a1Fields.size(), equalTo(2));
            assertThat(a1Fields.get(0).getName(), equalTo("numerator"));
            assertThat(a1Fields.get(0).getValue(), equalTo(1));
            assertThat(a1Fields.get(1).getName(), equalTo("denominator"));
            assertThat(a1Fields.get(1).getValue(), equalTo(1));

            RuntimeValue argument2 = returnValue.getArguments().get(1);
            assertThat(argument2.getName(), equalTo("b"));

            List<RuntimeValue> a2Fields = argument2.getFields();
            assertThat(a2Fields.size(), equalTo(2));
            assertThat(a2Fields.get(0).getName(), equalTo("numerator"));
            assertThat(a2Fields.get(0).getValue(), equalTo(1));
            assertThat(a2Fields.get(1).getName(), equalTo("denominator"));
            assertThat(a2Fields.get(1).getValue(), equalTo(4));
        }
    }

    @Nested
    class DeepCopyArray_ItMayMutateLater {
        @Test
        void nonPrimitive() throws MavenInvocationException, IOException {
            // act
            File pomFile = new File("src/test/resources/copy-array/pom.xml");
            InvocationResult result = getInvocationResult(
                    pomFile,
                    List.of(
                            "classesAndBreakpoints=src/test/resources/non-primitive.txt",
                            "output=target/output.json",
                            "executionDepth=0"),
                    "-Dtest=CopyArrayTest#nonPrimitive");

            // assert
            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/copy-array/target/output.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assertThat(output.getBreakpoint().size(), equalTo(1));
            assertThat(output.getReturns().size(), equalTo(0));

            StackFrameContext stackFrameContext =
                    output.getBreakpoint().get(0).getStackFrameContext().get(0);
            RuntimeValue localVariable =
                    stackFrameContext.getRuntimeValueCollection().get(0);
            assertThat(localVariable.getName(), equalTo("chronologies"));
            assertThat(localVariable.getValue(), equalTo(new ArrayList<>(Collections.nCopies(4, null))));
        }

        @Test
        void primitive() throws MavenInvocationException, IOException {
            // act
            File pomFile = new File("src/test/resources/copy-array/pom.xml");
            InvocationResult result = getInvocationResult(
                    pomFile,
                    List.of(
                            "classesAndBreakpoints=src/test/resources/primitive.txt",
                            "output=target/output.json",
                            "executionDepth=0"),
                    "-Dtest=CopyArrayTest#primitive");

            // assert
            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/copy-array/target/output.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assertThat(output.getBreakpoint().size(), equalTo(1));
            assertThat(output.getReturns().size(), equalTo(0));

            LineSnapshot lineSnapshot = output.getBreakpoint().get(0);
            assertThat(lineSnapshot.getLineNumber(), equalTo(6));

            StackFrameContext stackFrameContext =
                    lineSnapshot.getStackFrameContext().get(0);
            RuntimeValue localVariable =
                    stackFrameContext.getRuntimeValueCollection().get(0);
            assertThat(localVariable.getName(), equalTo("array"));
            assertThat(localVariable.getValue(), equalTo(List.of(0, 0, 0, 0)));
        }
    }

    private InvocationResult getInvocationResult(File pomFile, List<String> agentOptions, String testArg)
            throws MavenInvocationException, IOException {
        // arrange
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomFile);
        request.setGoals(List.of("clean", "test"));
        request.addArg(testArg);
        request.addArg("-DargLine=-javaagent:" + getAgentPath() + "=" + String.join(",", agentOptions));

        // act
        Invoker invoker = new DefaultInvoker();
        return invoker.execute(request);
    }
}
