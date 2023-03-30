import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.assertteam.Classes;
import se.assertteam.LineSnapshot;
import se.assertteam.RuntimeValue;
import se.assertteam.SahabOutput;
import se.assertteam.StackFrameContext;

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
        assertThat(runtimeValue0.getType(), equalTo(int.class));

        RuntimeValue runtimeValue1 = runtimeValues.get(1);
        assertThat(runtimeValue1.getValue(), equalTo(2));
        assertThat(runtimeValue1.getType(), equalTo(int.class));
        // ToDo: Add feature for collecting return values
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
            assertThat(runtimeValue0_11.getType(), equalTo(Float.class));

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
            assertThat(runtimeValue0_6.getType(), equalTo(Double.class));

            // Line 7
            assertThat(snapshot3.getLineNumber(), equalTo(7));
            theOnlyStackFrameContext = snapshot3.getStackFrameContext().get(0);
            assertThat(theOnlyStackFrameContext.getRuntimeValueCollection().size(), equalTo(2));
            RuntimeValue runtimeValue1 =
                    theOnlyStackFrameContext.getRuntimeValueCollection().get(1);
            assertThat(runtimeValue1.getName(), equalTo("negativeInfinity"));
            assertThat(runtimeValue1.getValue(), equalTo(Objects.toString(Double.NEGATIVE_INFINITY)));
            assertThat(runtimeValue1.getType(), equalTo(Double.class));
        }

        @Test
        void shouldBeAbleToSerialise_nestedSpecialFloatingPointValue() throws MavenInvocationException, IOException {
            // act
            InvocationResult result = getInvocationResult(
                    new File("src/test/resources/special-floating-point-value/pom.xml"),
                    List.of("classesAndBreakpoints=src/test/resources/nested.txt", "output=target/nested.json"),
                    "");

            // assert
            assertThat(result.getExitCode(), equalTo(0));
            File actualOutput = new File("src/test/resources/special-floating-point-value/target/nested.json");
            assertThat(actualOutput.exists(), equalTo(true));

            ObjectMapper mapper = new ObjectMapper();
            SahabOutput output = mapper.readValue(actualOutput, new TypeReference<>() {});
            assumeFalse(output.getReturns().size() == 1);

            // ToDo: Add feature for collecting return values
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

            RuntimeValue firstElement = elementsOfQueue.getArrayElements().get(0);
            assertThat(firstElement.getValue(), equalTo("Added at runtime"));

            // static fields
            // list
            RuntimeValue list =
                    theOnlyStackTraceContext.getRuntimeValueCollection().get(1);
            assertThat(list.getName(), equalTo("list"));
            RuntimeValue elementsOfList = list.getFields().get(0);

            List<Object> atomicValue = elementsOfList.getArrayElements().stream()
                    .map(RuntimeValue::getValue)
                    .collect(Collectors.toList());
            assertThat(atomicValue, equalTo(List.of(1, 2, 3, 4, 5)));

            // set
            RuntimeValue set =
                    theOnlyStackTraceContext.getRuntimeValueCollection().get(2);
            assertThat(set.getName(), equalTo("set"));
            RuntimeValue elementsOfSet = set.getFields().get(0);

            List<Object> atomicValuesInSet = elementsOfSet.getArrayElements().stream()
                    .map(RuntimeValue::getValue)
                    .collect(Collectors.toList());
            // null are pre-allocated buffers in HashSet
            assertThat(atomicValuesInSet, containsInAnyOrder("aman", "sharma", "sahab", null, null, null));
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

            List<RuntimeValue> stringsInsideArray = runtimeValues.get(0).getArrayElements();
            List<Object> atomicValuesInSet =
                    stringsInsideArray.stream().map(RuntimeValue::getValue).collect(Collectors.toList());
            assertThat(atomicValuesInSet, equalTo(List.of("yes", "we", "can")));
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
                assertThat(nestedArray.getType(), equalTo(Classes.getClassFromString("int[][][]")));
                assertThat(nestedArray.getArrayElements(), is(empty()));
            }

            @Test
            void nestedArray_depth1() throws MavenInvocationException, IOException {
                // arrange
                RuntimeValue nestedArray = getStackFrameContext(1);
                assertThat(nestedArray.getArrayElements().size(), equalTo(2));

                RuntimeValue array0 = nestedArray.getArrayElements().get(0);
                assertThat(array0.getKind(), equalTo(RuntimeValue.Kind.ARRAY_ELEMENT));
                assertThat(array0.getType(), equalTo(Classes.getClassFromString("int[][]")));
                assertThat(array0.getArrayElements(), is(empty()));

                RuntimeValue array1 = nestedArray.getArrayElements().get(1);
                assertThat(array1.getKind(), equalTo(RuntimeValue.Kind.ARRAY_ELEMENT));
                assertThat(array1.getType(), equalTo(Classes.getClassFromString("int[][]")));
                assertThat(array1.getArrayElements(), is(empty()));
            }

            @Test
            void nestedArray_depth2() throws MavenInvocationException, IOException {
                // arrange
                RuntimeValue nestedArray = getStackFrameContext(2);

                // assert
                RuntimeValue array0 =
                        nestedArray.getArrayElements().get(0).getArrayElements().get(0);
                assertThat(array0.getKind(), equalTo(RuntimeValue.Kind.ARRAY_ELEMENT));
                assertThat(array0.getType(), equalTo(Classes.getClassFromString("int[]")));
                assertThat(array0.getArrayElements(), is(empty()));

                RuntimeValue array1 =
                        nestedArray.getArrayElements().get(0).getArrayElements().get(1);
                assertThat(array1.getKind(), equalTo(RuntimeValue.Kind.ARRAY_ELEMENT));
                assertThat(array1.getType(), equalTo(Classes.getClassFromString("int[]")));
                assertThat(array1.getArrayElements(), is(empty()));

                RuntimeValue array2 =
                        nestedArray.getArrayElements().get(1).getArrayElements().get(0);
                assertThat(array2.getKind(), equalTo(RuntimeValue.Kind.ARRAY_ELEMENT));
                assertThat(array2.getType(), equalTo(Classes.getClassFromString("int[]")));
                assertThat(array2.getArrayElements(), is(empty()));

                RuntimeValue array3 =
                        nestedArray.getArrayElements().get(1).getArrayElements().get(1);
                assertThat(array3.getKind(), equalTo(RuntimeValue.Kind.ARRAY_ELEMENT));
                assertThat(array3.getType(), equalTo(Classes.getClassFromString("int[]")));
                assertThat(array3.getArrayElements(), is(empty()));
            }

            @Test
            void nestedArray_depth3() throws MavenInvocationException, IOException {
                // arrange
                RuntimeValue nestedArray = getStackFrameContext(3);

                // assert
                RuntimeValue array0 =
                        nestedArray.getArrayElements().get(0).getArrayElements().get(0);
                List<Object> actualValues_0 = array0.getArrayElements().stream()
                        .map(RuntimeValue::getValue)
                        .collect(Collectors.toList());
                assertThat(actualValues_0, equalTo(List.of(1)));

                RuntimeValue array1 =
                        nestedArray.getArrayElements().get(0).getArrayElements().get(1);
                List<Object> actualValues_1 = array1.getArrayElements().stream()
                        .map(RuntimeValue::getValue)
                        .collect(Collectors.toList());
                assertThat(actualValues_1, equalTo(List.of(2)));

                RuntimeValue array2 =
                        nestedArray.getArrayElements().get(1).getArrayElements().get(0);
                List<Object> actualValues_2 = array2.getArrayElements().stream()
                        .map(RuntimeValue::getValue)
                        .collect(Collectors.toList());
                assertThat(actualValues_2, equalTo(List.of(3, 4, 5)));

                RuntimeValue array3 =
                        nestedArray.getArrayElements().get(1).getArrayElements().get(1);
                List<Object> actualValues_3 = array3.getArrayElements().stream()
                        .map(RuntimeValue::getValue)
                        .collect(Collectors.toList());
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

    private InvocationResult getInvocationResult(File pomFile, List<String> agentOptions, String testArg)
            throws MavenInvocationException {
        // arrange
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomFile);
        request.setGoals(List.of("clean", "test"));
        request.addArg(testArg);
        request.addArg("-DargLine=-javaagent:../../../../target/collector-sahab.jar=" + String.join(",", agentOptions));

        // act
        Invoker invoker = new DefaultInvoker();
        return invoker.execute(request);
    }
}
