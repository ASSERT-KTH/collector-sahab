package io.github.chains_project.cs.preprocess;

import static io.github.chains_project.collector.util.JavaAgentPath.getAgentPath;

import io.github.chains_project.cs.commons.CollectorAgentOptions;
import io.github.chains_project.cs.commons.Revision;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class PomTransformer {

    private final Revision revision;
    private final Model model;

    private final CollectorAgentOptions options;

    public static String AGENT_JAR;

    private static final List<String> OLD_COMPILER_VERSIONS = List.of("1.5", "5");

    static {
        try {
            AGENT_JAR = getAgentPath();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not fetch trace-collector.jar. Please package `trace-collector` module again.");
        }
    }

    public PomTransformer(Revision revision, CollectorAgentOptions options, List<String> tests)
            throws IOException, XmlPullParserException {
        this.revision = revision;
        this.options = options;
        Path pomFile = revision.resolveFilename("pom.xml");
        MavenXpp3Reader reader = new MavenXpp3Reader();
        this.model = reader.read(new FileReader(pomFile.toFile(), StandardCharsets.UTF_8));
        preprocessPom();
        modifyProperties();
        modifySurefirePlugin(tests);
        modifyCompilerPlugin();
        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(new FileWriter(pomFile.toFile(), StandardCharsets.UTF_8), model);
    }

    public void modifyProperties() {
        Properties properties = model.getProperties();
        if (properties == null) {
            return;
        }
        String mavenCompilerCompilerVersion = properties.getProperty("maven.compiler.compilerVersion");
        if (mavenCompilerCompilerVersion != null && OLD_COMPILER_VERSIONS.contains(mavenCompilerCompilerVersion)) {
            properties.setProperty("maven.compiler.compilerVersion", "1.6");
        }
        String mavenCompileCompilerVersion = properties.getProperty("maven.compile.compilerVersion");
        if (mavenCompileCompilerVersion != null && OLD_COMPILER_VERSIONS.contains(mavenCompileCompilerVersion)) {
            properties.setProperty("maven.compile.compilerVersion", "1.6");
        }
        String mavenCompilerSource = properties.getProperty("maven.compiler.source");
        if (mavenCompilerSource != null && OLD_COMPILER_VERSIONS.contains(mavenCompilerSource)) {
            properties.setProperty("maven.compiler.source", "1.6");
        }
        String mavenCompileSource = properties.getProperty("maven.compile.source");
        if (mavenCompileSource != null && OLD_COMPILER_VERSIONS.contains(mavenCompileSource)) {
            properties.setProperty("maven.compile.source", "1.6");
        }
        String mavenCompilerTarget = properties.getProperty("maven.compiler.target");
        if (mavenCompilerTarget != null && OLD_COMPILER_VERSIONS.contains(mavenCompilerTarget)) {
            properties.setProperty("maven.compiler.target", "1.6");
        }
        String mavenCompileTarget = properties.getProperty("maven.compile.target");
        if (mavenCompileTarget != null && OLD_COMPILER_VERSIONS.contains(mavenCompileTarget)) {
            properties.setProperty("maven.compile.target", "1.6");
        }
    }

    private void preprocessPom() {
        if (model.getBuild() == null) {
            model.setBuild(new Build());
        } else if (model.getBuild().getPlugins() == null) {
            model.getBuild().setPlugins(new ArrayList<>());
        }
    }

    public PomTransformer(Path pomFile) throws IOException, XmlPullParserException {
        this.revision = null;
        this.options = new CollectorAgentOptions();
        MavenXpp3Reader reader = new MavenXpp3Reader();
        this.model = reader.read(new FileReader(pomFile.toFile(), StandardCharsets.UTF_8));
    }

    public void modifySurefirePlugin(List<String> tests) {
        Build build = model.getBuild();
        Optional<Plugin> candidate = build.getPlugins().stream()
                .filter(plugin -> "maven-surefire-plugin".equals(plugin.getArtifactId()))
                .findFirst();

        String sanitizedTests = parseTestsForSurefire(tests);

        if (candidate.isPresent()) {
            Plugin plugin = candidate.get();
            Object configuration = plugin.getConfiguration();
            Xpp3Dom modifiedConfig = getModifiedSurefireConfiguration((Xpp3Dom) configuration, options, sanitizedTests);
            plugin.setConfiguration(modifiedConfig);
        } else {
            Plugin surefirePlugin = new Plugin();
            surefirePlugin.setGroupId("org.apache.maven.plugins");
            surefirePlugin.setArtifactId("maven-surefire-plugin");
            surefirePlugin.setVersion("3.0.0");

            Xpp3Dom configuration = new Xpp3Dom("configuration");
            Xpp3Dom argLine = new Xpp3Dom("argLine");

            if (!sanitizedTests.isEmpty()) {
                Xpp3Dom test = new Xpp3Dom("test");
                test.setValue(sanitizedTests);
                configuration.addChild(test);
            }

            // Some modules may not have any tests, so we prevent its build from failing.
            addIfDoesNotExist("failIfNoTests", "false", configuration);
            // The build should continue even if a test fails because we still need the data from instrumentations.
            addIfDoesNotExist("testFailureIgnore", "true", configuration);
            // Some modules may not have the specified tests, so we prevent its build from failing.
            addIfDoesNotExist("failIfNoSpecifiedTests", "false", configuration);

            argLine.setValue("-javaagent:" + AGENT_JAR + "=" + options.toString());
            configuration.addChild(argLine);
            surefirePlugin.setConfiguration(configuration);
            build.getPlugins().add(surefirePlugin);
        }
    }

    private static String parseTestsForSurefire(List<String> tests) {
        List<String> sanitizedTests =
                tests.stream().map(test -> test.replace("::", "#")).collect(Collectors.toList());
        return String.join(",", sanitizedTests);
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

    private static Xpp3Dom getModifiedSurefireConfiguration(
            Xpp3Dom configuration, CollectorAgentOptions options, String sanitizedTests) {
        //        <configuration>
        //
        // <argLine>-javaagent:../../../../target/trace-collector.jar=classesAndBreakpoints=src/test/resources/basic-math.txt,methodsForExitEvent=src/test/resources/basic-math.json</argLine>
        //        </configuration>
        if (configuration == null) {
            configuration = new Xpp3Dom("configuration");
        }
        Xpp3Dom argLine = configuration.getChild("argLine");
        if (!sanitizedTests.isEmpty()) {
            Xpp3Dom testNode = new Xpp3Dom("test");
            testNode.setValue(sanitizedTests);
            configuration.addChild(testNode);
        }
        if (argLine == null) {
            argLine = new Xpp3Dom("argLine");
            argLine.setValue("-javaagent:" + AGENT_JAR + "=" + options.toString());
            configuration.addChild(argLine);
        } else {
            argLine.setValue("-javaagent:" + AGENT_JAR + "=" + options.toString() + " " + argLine.getValue());
        }

        addIfDoesNotExist("failIfNoTests", "false", configuration);
        addIfDoesNotExist("testFailureIgnore", "true", configuration);
        addIfDoesNotExist("failIfNoSpecifiedTests", "false", configuration);

        return configuration;
    }

    private static void addIfDoesNotExist(String attribute, String value, Xpp3Dom configuration) {
        Xpp3Dom attributeNode = configuration.getChild(attribute);
        if (attributeNode == null) {
            attributeNode = new Xpp3Dom(attribute);
            attributeNode.setValue(value);
            configuration.addChild(attributeNode);
        } else {
            attributeNode.setValue(value);
        }
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
        // By default, the compiler plugin compiles debug information.
        Xpp3Dom debug = configuration.getChild("debug");
        if (debug != null) {
            configuration.removeChild(debug);
        }

        Xpp3Dom debugLevel = configuration.getChild("debuglevel");
        if (debugLevel != null) {
            configuration.removeChild(debugLevel);
        }

        Xpp3Dom source = configuration.getChild("source");
        if (source != null && ("5".equals(source.getValue()) || "1.5".equals(source.getValue()))) {
            source.setValue("1.6");
        }
        Xpp3Dom target = configuration.getChild("target");
        if (target != null && ("5".equals(target.getValue()) || "1.5".equals(target.getValue()))) {
            target.setValue("1.6");
        }
        return configuration;
    }

    public Model getModel() {
        return model;
    }
}
