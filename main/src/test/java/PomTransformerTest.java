import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.assertkth.cs.preprocess.PomTransformer;

class PomTransformerTest {

    @BeforeAll
    static void setup() {
        PomTransformer.AGENT_JAR = "trace-collector.jar";
    }

    @Nested
    class Surefire {

        @Test
        void modifySurefirePlugin_shouldAppend() throws XmlPullParserException, IOException {
            // arrange;
            PomTransformer transformer = new PomTransformer(Paths.get("src/test/resources/surefire/append.xml"));

            assertThat(transformer.getModel().getBuild().getPlugins().size(), is(equalTo(1)));

            // act
            transformer.modifySurefirePlugin();
            Model transformedModel = transformer.getModel();

            // assert
            assertThat(transformedModel.getBuild().getPlugins().size(), is(equalTo(2)));
            Plugin existingPlugin = transformedModel.getBuild().getPlugins().get(0);
            assertThat(existingPlugin.getArtifactId(), is(equalTo("maven-compiler-plugin")));

            Plugin addedPlugin = transformedModel.getBuild().getPlugins().get(1);
            assertThat(addedPlugin.getArtifactId(), is(equalTo("maven-surefire-plugin")));
            assertThat(addedPlugin.getGroupId(), is(equalTo("org.apache.maven.plugins")));
            assertThat(addedPlugin.getVersion(), is(equalTo("3.0.0")));

            Xpp3Dom configuration = (Xpp3Dom) addedPlugin.getConfiguration();
            assertThat(configuration.getChildCount(), is(equalTo(1)));
            assertThat(
                    configuration.getChild("argLine").getValue(),
                    is(
                            equalTo(
                                    "-javaagent:trace-collector.jar=classesAndBreakpoints=null,methodsForExitEvent=null,output=target/output.json,executionDepth=0,numberOfArrayElements=10,extractParameters=false")));
        }

        @Test
        void modifySurefirePlugin_shouldModify() throws XmlPullParserException, IOException {
            // arrange;
            PomTransformer transformer = new PomTransformer(Paths.get("src/test/resources/surefire/modify.xml"));

            assertThat(transformer.getModel().getBuild().getPlugins().size(), is(equalTo(1)));
            Plugin originalPlugin =
                    transformer.getModel().getBuild().getPlugins().get(0);
            Xpp3Dom oldConfiguration = ((Xpp3Dom) originalPlugin.getConfiguration());

            assertThat(oldConfiguration.getChildCount(), is(equalTo(1)));
            assertThat(
                    oldConfiguration.getChild("argLine").getValue(),
                    is(equalTo("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")));

            // act
            transformer.modifySurefirePlugin();
            Model transformedModel = transformer.getModel();

            // assert
            assertThat(transformedModel.getBuild().getPlugins().size(), is(equalTo(1)));
            Plugin modifiedPlugin = transformedModel.getBuild().getPlugins().get(0);

            Xpp3Dom configuration = (Xpp3Dom) modifiedPlugin.getConfiguration();
            assertThat(configuration.getChildCount(), is(equalTo(1)));
            assertThat(
                    configuration.getChild("argLine").getValue(),
                    is(
                            equalTo(
                                    "-javaagent:trace-collector.jar=classesAndBreakpoints=null,methodsForExitEvent=null,output=target/output.json,executionDepth=0,numberOfArrayElements=10,extractParameters=false -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")));
        }

        @Test
        void modifySurefirePlugin_shouldAddConfiguration() throws XmlPullParserException, IOException {
            // arrange;
            PomTransformer transformer =
                    new PomTransformer(Paths.get("src/test/resources/surefire/add-configuration.xml"));

            Plugin surefire = transformer.getModel().getBuild().getPlugins().get(0);
            assertThat(surefire.getConfiguration(), is(equalTo(null)));

            // act
            transformer.modifySurefirePlugin();
            Model transformedModel = transformer.getModel();

            // assert
            Xpp3Dom configuration = (Xpp3Dom) surefire.getConfiguration();
            assertThat(configuration.getChildCount(), is(equalTo(1)));
            assertThat(
                    configuration.getChild("argLine").getValue(),
                    is(
                            equalTo(
                                    "-javaagent:trace-collector.jar=classesAndBreakpoints=null,methodsForExitEvent=null,output=target/output.json,executionDepth=0,numberOfArrayElements=10,extractParameters=false")));
        }

        @Test
        void modifySurefirePlugin_printedSurefirePluginShouldBeModified(@TempDir Path tempDir)
                throws XmlPullParserException, IOException {
            // arrange;
            Path originalPom = Paths.get("src/test/resources/surefire/modify.xml");
            PomTransformer transformer = new PomTransformer(originalPom);

            // act
            transformer.modifySurefirePlugin();
            Path actualPom = tempDir.resolve("modify.xml");
            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(new FileWriter(actualPom.toFile()), transformer.getModel());

            // assert
            String originalPomString = Files.readString(originalPom);
            String actualPomString = Files.readString(actualPom);
            String argLine =
                    "<argLine>-javaagent:trace-collector.jar=classesAndBreakpoints=null,methodsForExitEvent=null,output=target/output.json,executionDepth=0,numberOfArrayElements=10,extractParameters=false -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005</argLine>";
            assertThat(originalPomString, not(containsString(argLine)));
            assertThat(actualPomString, containsString(argLine));
        }
    }

    @Nested
    class Compiler {
        @Test
        void modifyCompilerPlugin_shouldAppend() throws XmlPullParserException, IOException {
            // arrange;
            PomTransformer transformer = new PomTransformer(Paths.get("src/test/resources/compiler/append.xml"));

            assertThat(transformer.getModel().getBuild().getPlugins().size(), is(equalTo(1)));

            // act
            transformer.modifyCompilerPlugin();
            Model transformedModel = transformer.getModel();

            // assert
            assertThat(transformedModel.getBuild().getPlugins().size(), is(equalTo(2)));
            Plugin existingPlugin = transformedModel.getBuild().getPlugins().get(0);
            assertThat(existingPlugin.getArtifactId(), is(equalTo("maven-surefire-plugin")));

            Plugin addedPlugin = transformedModel.getBuild().getPlugins().get(1);
            assertThat(addedPlugin.getArtifactId(), is(equalTo("maven-compiler-plugin")));
            assertThat(addedPlugin.getGroupId(), is(equalTo("org.apache.maven.plugins")));
            assertThat(addedPlugin.getVersion(), is(equalTo("3.11.0")));

            Xpp3Dom configuration = (Xpp3Dom) addedPlugin.getConfiguration();
            assertThat(configuration.getChildCount(), is(equalTo(1)));
            Xpp3Dom compilerArgs = configuration.getChild("compilerArgs");
            Xpp3Dom arg;
            for (int i = 0; i < compilerArgs.getChildCount(); i++) {
                arg = compilerArgs.getChild(i);
                if (arg.getValue().equals("-parameters")) {
                    assertThat(arg.getValue(), is(equalTo("-parameters")));
                    return;
                }
            }
            fail("Did not find -parameters in compilerArgs");
        }

        @Test
        void modifyCompilerPlugin_shouldLeaveAsIs(@TempDir Path tempDir) throws XmlPullParserException, IOException {
            // arrange;
            PomTransformer transformer = new PomTransformer(Paths.get("src/test/resources/compiler/no-op.xml"));
            Model expectedModel = transformer.getModel();
            expectedModel = expectedModel.clone();
            Xpp3Dom expectedCompilerArgs = ((Xpp3Dom)
                            expectedModel.getBuild().getPlugins().get(0).getConfiguration())
                    .getChild("compilerArgs");

            // act
            transformer.modifyCompilerPlugin();
            Model transformedModel = transformer.getModel();

            // assert
            Xpp3Dom transformedCompilerArgs = ((Xpp3Dom)
                            transformedModel.getBuild().getPlugins().get(0).getConfiguration())
                    .getChild("compilerArgs");

            assertThat(transformedCompilerArgs, is(equalTo(expectedCompilerArgs)));
        }

        @Test
        void modifyCompilerPlugin_shouldAppendToArgs(@TempDir Path tempDir) throws XmlPullParserException, IOException {
            // arrange;
            PomTransformer transformer = new PomTransformer(Paths.get("src/test/resources/compiler/modify.xml"));

            Plugin compilerPlugin =
                    transformer.getModel().getBuild().getPlugins().get(0);
            Xpp3Dom compilerArgs = ((Xpp3Dom) compilerPlugin.getConfiguration()).getChild("compilerArgs");
            assertThat(compilerArgs.getChildCount(), is(equalTo(1)));

            // act
            transformer.modifyCompilerPlugin();

            // assert
            compilerArgs = ((Xpp3Dom) compilerPlugin.getConfiguration()).getChild("compilerArgs");
            assertThat(compilerArgs.getChildCount(), is(equalTo(2)));
            Xpp3Dom noWarn = compilerArgs.getChild(0);
            assertThat(noWarn.getValue(), is(equalTo("-nowarn")));
            Xpp3Dom parameters = compilerArgs.getChild(1);
            assertThat(parameters.getValue(), is(equalTo("-parameters")));
        }
    }
}