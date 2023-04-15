import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;
import static se.assertkth.cs.preprocess.JavaAgentPath.getAgentPath;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.assertkth.cs.commons.Pair;
import se.assertkth.cs.preprocess.PomTransformer;

class PomTransformerTest {

    @Nested
    class Surefire {

        @Test
        void modifySurefirePlugin_shouldAppend() throws XmlPullParserException, IOException {
            // arrange;
            PomTransformer transformer = new PomTransformer(Paths.get("src/test/resources/surefire/append.xml"));

            assertThat(transformer.getModel().getBuild().getPlugins().size(), is(equalTo(1)));

            // act
            transformer.modifySurefirePlugin(List.of());
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
            assertThat(configuration.getChildCount(), is(equalTo(4)));

            Xpp3Dom[] children = configuration.getChildren();
            List<Pair> attributes = Arrays.stream(children)
                    .map(child -> new Pair(child.getName(), child.getValue()))
                    .collect(Collectors.toList());
            assertThat(
                    attributes,
                    hasItems(
                            new Pair<>("failIfNoTests", "false"),
                            new Pair<>("testFailureIgnore", "true"),
                            new Pair<>("failIfNoSpecifiedTests", "false")));

            assertThat(
                    configuration.getChild("argLine").getValue(),
                    is(
                            equalTo(
                                    "-javaagent:" + getAgentPath()
                                            + "=classesAndBreakpoints=null,methodsForExitEvent=null,output=target/output.json,executionDepth=0,numberOfArrayElements=10,extractParameters=false")));
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
            transformer.modifySurefirePlugin(List.of());
            Model transformedModel = transformer.getModel();

            // assert
            assertThat(transformedModel.getBuild().getPlugins().size(), is(equalTo(1)));
            Plugin modifiedPlugin = transformedModel.getBuild().getPlugins().get(0);

            Xpp3Dom configuration = (Xpp3Dom) modifiedPlugin.getConfiguration();
            assertThat(configuration.getChildCount(), is(equalTo(4)));
            Xpp3Dom[] children = configuration.getChildren();
            List<Pair> attributes = Arrays.stream(children)
                    .map(child -> new Pair(child.getName(), child.getValue()))
                    .collect(Collectors.toList());
            assertThat(
                    attributes,
                    hasItems(
                            new Pair<>("failIfNoTests", "false"),
                            new Pair<>("testFailureIgnore", "true"),
                            new Pair<>("failIfNoSpecifiedTests", "false")));
            assertThat(
                    configuration.getChild("argLine").getValue(),
                    is(
                            equalTo(
                                    "-javaagent:" + getAgentPath()
                                            + "=classesAndBreakpoints=null,methodsForExitEvent=null,output=target/output.json,executionDepth=0,numberOfArrayElements=10,extractParameters=false -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")));
        }

        @Test
        void modifySurefirePlugin_shouldAddConfiguration() throws XmlPullParserException, IOException {
            // arrange;
            PomTransformer transformer =
                    new PomTransformer(Paths.get("src/test/resources/surefire/add-configuration.xml"));

            Plugin surefire = transformer.getModel().getBuild().getPlugins().get(0);
            assertThat(surefire.getConfiguration(), is(equalTo(null)));

            // act
            transformer.modifySurefirePlugin(List.of());

            // assert
            Xpp3Dom configuration = (Xpp3Dom) surefire.getConfiguration();
            assertThat(configuration.getChildCount(), is(equalTo(4)));
            Xpp3Dom[] children = configuration.getChildren();
            List<Pair> attributes = Arrays.stream(children)
                    .map(child -> new Pair(child.getName(), child.getValue()))
                    .collect(Collectors.toList());
            assertThat(
                    attributes,
                    hasItems(
                            new Pair<>("failIfNoTests", "false"),
                            new Pair<>("testFailureIgnore", "true"),
                            new Pair<>("failIfNoSpecifiedTests", "false")));
            // last child which is argLine
            assertThat(
                    configuration.getChild("argLine").getValue(),
                    is(
                            equalTo(
                                    "-javaagent:" + getAgentPath()
                                            + "=classesAndBreakpoints=null,methodsForExitEvent=null,output=target/output.json,executionDepth=0,numberOfArrayElements=10,extractParameters=false")));
        }

        @Test
        void modifySurefirePlugin_printedSurefirePluginShouldBeModified(@TempDir Path tempDir)
                throws XmlPullParserException, IOException {
            // arrange;
            Path originalPom = Paths.get("src/test/resources/surefire/modify.xml");
            PomTransformer transformer = new PomTransformer(originalPom);

            // act
            transformer.modifySurefirePlugin(List.of());
            Path actualPom = tempDir.resolve("modify.xml");
            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(new FileWriter(actualPom.toFile()), transformer.getModel());

            // assert
            String originalPomString = Files.readString(originalPom);
            String actualPomString = Files.readString(actualPom);
            String argLine = "<argLine>-javaagent:" + getAgentPath()
                    + "=classesAndBreakpoints=null,methodsForExitEvent=null,output=target/output.json,executionDepth=0,numberOfArrayElements=10,extractParameters=false -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005</argLine>";
            assertThat(originalPomString, not(containsString(argLine)));
            assertThat(actualPomString, containsString(argLine));
        }

        @Test
        void modifySurefirePlugin_addTestsToExistingPlugin() throws XmlPullParserException, IOException {
            // arrange;
            Path originalPom = Paths.get("src/test/resources/surefire/add-tests-to-existing-plugin.xml");
            PomTransformer transformer = new PomTransformer(originalPom);
            Plugin surefire = transformer.getModel().getBuild().getPlugins().get(0);

            // act
            transformer.modifySurefirePlugin(List.of("com.example.Test1", "com.example.Test2::testMethod"));

            // assert
            Xpp3Dom configuration = (Xpp3Dom) surefire.getConfiguration();
            Xpp3Dom test = configuration.getChild("test");
            assertThat(test.getValue(), is(equalTo("com.example.Test1,com.example.Test2#testMethod")));
        }

        @Test
        void modifySurefirePlugin_addTestsToNewPlugin() throws XmlPullParserException, IOException {
            // arrange;
            Path originalPom = Paths.get("src/test/resources/surefire/add-tests-to-new-plugin.xml");
            PomTransformer transformer = new PomTransformer(originalPom);
            assertThat(transformer.getModel().getBuild().getPlugins(), is(empty()));

            // act
            transformer.modifySurefirePlugin(List.of("se.kth.A$B", "com.example.Test2#testMethod"));

            // assert
            Plugin surefire = transformer.getModel().getBuild().getPlugins().get(0);
            Xpp3Dom configuration = (Xpp3Dom) surefire.getConfiguration();
            Xpp3Dom test = configuration.getChild("test");
            assertThat(test.getValue(), is(equalTo("se.kth.A$B,com.example.Test2#testMethod")));
        }

        @Test
        void modifySurefirePlugin_modifyAttributes() throws XmlPullParserException, IOException {
            // arrange;
            Path originalPom = Paths.get("src/test/resources/surefire/modify-attributes.xml");
            PomTransformer transformer = new PomTransformer(originalPom);
            Plugin surefire = transformer.getModel().getBuild().getPlugins().get(0);
            Xpp3Dom configuration = (Xpp3Dom) surefire.getConfiguration();

            Xpp3Dom failIfNoTests = configuration.getChild("failIfNoTests");
            assertThat(failIfNoTests.getValue(), is(equalTo("true")));

            Xpp3Dom testFailureIgnore = configuration.getChild("testFailureIgnore");
            assertThat(testFailureIgnore.getValue(), is(equalTo("false")));

            Xpp3Dom failIfNoSpecifiedTests = configuration.getChild("failIfNoSpecifiedTests");
            assertThat(failIfNoSpecifiedTests.getValue(), is(equalTo("true")));

            // act
            transformer.modifySurefirePlugin(List.of());

            // assert
            configuration = (Xpp3Dom) surefire.getConfiguration();
            failIfNoTests = configuration.getChild("failIfNoTests");
            assertThat(failIfNoTests.getValue(), is(equalTo("false")));

            testFailureIgnore = configuration.getChild("testFailureIgnore");
            assertThat(testFailureIgnore.getValue(), is(equalTo("true")));

            failIfNoSpecifiedTests = configuration.getChild("failIfNoSpecifiedTests");
            assertThat(failIfNoSpecifiedTests.getValue(), is(equalTo("false")));
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
