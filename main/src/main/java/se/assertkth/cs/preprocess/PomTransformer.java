package se.assertkth.cs.preprocess;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class PomTransformer {
    private final Model model;

    public PomTransformer(Path pomFile) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        this.model = reader.read(new FileReader(pomFile.toFile(), StandardCharsets.UTF_8));
    }

    public void modifySurefirePlugin() {
        Build build = model.getBuild();
        Optional<Plugin> candidate = build.getPlugins().stream()
                .filter(plugin -> "maven-surefire-plugin".equals(plugin.getArtifactId()))
                .findFirst();
        if (candidate.isPresent()) {
            Plugin plugin = candidate.get();
            Object configuration = plugin.getConfiguration();
            Xpp3Dom modifiedConfig = getModifiedSurefireConfiguration((Xpp3Dom) configuration);
            plugin.setConfiguration(modifiedConfig);
        } else {
            Plugin surefirePlugin = new Plugin();
            surefirePlugin.setGroupId("org.apache.maven.plugins");
            surefirePlugin.setArtifactId("maven-surefire-plugin");
            surefirePlugin.setVersion("3.0.0");

            Xpp3Dom configuration = new Xpp3Dom("configuration");
            Xpp3Dom argLine = new Xpp3Dom("argLine");
            argLine.setValue("-javaagent:trace-collector.jar=");
            configuration.addChild(argLine);
            surefirePlugin.setConfiguration(configuration);
            build.getPlugins().add(surefirePlugin);
        }
    }

    public void modifyCompilerPlugin() {
        Build build = model.getBuild();
        Optional<Plugin> candidate = build.getPlugins().stream()
                .filter(plugin -> "maven-compiler-plugin".equals(plugin.getArtifactId()))
                .findFirst();

        if (candidate.isPresent()) {
            Plugin plugin = candidate.get();
            Object configuration = plugin.getConfiguration();
            Xpp3Dom modifiedConfig = getModifiedCompilerConfiguration((Xpp3Dom) configuration);
            plugin.setConfiguration(modifiedConfig);
        } else {
            Plugin compilerPlugin = new Plugin();
            compilerPlugin.setGroupId("org.apache.maven.plugins");
            compilerPlugin.setArtifactId("maven-compiler-plugin");
            compilerPlugin.setVersion("3.11.0");

            Xpp3Dom configuration = new Xpp3Dom("configuration");
            Xpp3Dom compilerArgs = new Xpp3Dom("compilerArgs");
            Xpp3Dom arg = new Xpp3Dom("arg");
            arg.setValue("-parameters");
            compilerArgs.addChild(arg);
            configuration.addChild(compilerArgs);
            compilerPlugin.setConfiguration(configuration);
            build.getPlugins().add(compilerPlugin);
        }
    }

    private static Xpp3Dom getModifiedSurefireConfiguration(Xpp3Dom configuration) {
        //        <configuration>
        //
        // <argLine>-javaagent:../../../../target/trace-collector.jar=classesAndBreakpoints=src/test/resources/basic-math.txt,methodsForExitEvent=src/test/resources/basic-math.json</argLine>
        //        </configuration>
        if (configuration == null) {
            configuration = new Xpp3Dom("configuration");
        }
        Xpp3Dom argLine = configuration.getChild("argLine");
        if (argLine == null) {
            argLine = new Xpp3Dom("argLine");
            argLine.setValue("-javaagent:trace-collector.jar=");
            configuration.addChild(argLine);
        } else {
            argLine.setValue("-javaagent:trace-collector.jar= " + argLine.getValue());
        }
        return configuration;
    }

    private static Xpp3Dom getModifiedCompilerConfiguration(Xpp3Dom configuration) {
        //        <configuration>
        //          <compilerArgs>
        //            <arg>-parameters</arg>
        //          </compilerArgs>
        //        </configuration>
        if (configuration == null) {
            configuration = new Xpp3Dom("configuration");
        }
        Xpp3Dom compilerArgs = configuration.getChild("compilerArgs");
        Xpp3Dom arg = new Xpp3Dom("arg");
        arg.setValue("-parameters");
        if (compilerArgs == null) {
            compilerArgs = new Xpp3Dom("compilerArgs");
            compilerArgs.addChild(arg);
            configuration.addChild(compilerArgs);
        } else {
            boolean found = false;
            for (Xpp3Dom child : compilerArgs.getChildren()) {
                if ("-parameters".equals(child.getValue())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                compilerArgs.addChild(arg);
            }
        }
        return configuration;
    }

    public Model getModel() {
        return model;
    }
}
