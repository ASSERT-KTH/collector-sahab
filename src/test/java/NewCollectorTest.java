import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.assertteam.LineSnapshot;
import se.assertteam.RuntimeValue;
import se.assertteam.SahabOutput;
import se.assertteam.StackFrameContext;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class NewCollectorTest {
    @Test
    void basicMath_test() throws MavenInvocationException, IOException {
        // arrange
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("src/test/resources/basic-math/pom.xml"));
        request.setGoals(List.of("test"));

        // act
        Invoker invoker = new DefaultInvoker();
        InvocationResult result = invoker.execute(request);

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
        StackFrameContext theOnlyStackFrameContext = snapshot0.getStackFrameContext().get(0);
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
            // arrange
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(new File("src/test/resources/special-floating-point-value/pom.xml"));
            request.setGoals(List.of("clean","test"));
            List<String> agentOptions = List.of(
                    "classesAndBreakpoints=src/test/resources/linear.txt",
                    "output=target/linear.json"
            );
            request.addArg("-DargLine=-javaagent:../../../../target/collector-sahab.jar=" + String.join(",", agentOptions));

            // act
            Invoker invoker = new DefaultInvoker();
            InvocationResult result = invoker.execute(request);

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
            StackFrameContext theOnlyStackFrameContext = snapshot0.getStackFrameContext().get(0);
            assertThat(theOnlyStackFrameContext.getRuntimeValueCollection().size(), equalTo(2));
            RuntimeValue runtimeValue0_11 = theOnlyStackFrameContext.getRuntimeValueCollection().get(0);
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
            RuntimeValue runtimeValue0_6 = theOnlyStackFrameContext.getRuntimeValueCollection().get(0);
            assertThat(runtimeValue0_6.getName(), equalTo("positiveInfinity"));
            assertThat(runtimeValue0_6.getValue(), equalTo(Objects.toString(Double.POSITIVE_INFINITY)));
            assertThat(runtimeValue0_6.getType(), equalTo(Double.class));

            // Line 7
            assertThat(snapshot3.getLineNumber(), equalTo(7));
            theOnlyStackFrameContext = snapshot3.getStackFrameContext().get(0);
            assertThat(theOnlyStackFrameContext.getRuntimeValueCollection().size(), equalTo(2));
            RuntimeValue runtimeValue1 = theOnlyStackFrameContext.getRuntimeValueCollection().get(1);
            assertThat(runtimeValue1.getName(), equalTo("negativeInfinity"));
            assertThat(runtimeValue1.getValue(), equalTo(Objects.toString(Double.NEGATIVE_INFINITY)));
            assertThat(runtimeValue1.getType(), equalTo(Double.class));
        }

        @Test
        void shouldBeAbleToSerialise_nestedSpecialFloatingPointValue() throws MavenInvocationException, IOException {
            // arrange
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(new File("src/test/resources/special-floating-point-value/pom.xml"));
            request.setGoals(List.of("clean", "test"));
            List<String> agentOptions = List.of(
                    "classesAndBreakpoints=src/test/resources/nested.txt",
                    "output=target/nested.json"
            );
            request.addArg("-DargLine=-javaagent:../../../../target/collector-sahab.jar=" + String.join(",", agentOptions));

            // act
            Invoker invoker = new DefaultInvoker();
            InvocationResult result = invoker.execute(request);

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



}
