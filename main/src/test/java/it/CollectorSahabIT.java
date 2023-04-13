package it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.assertkth.cs.Main;

class CollectorSahabIT {

    @BeforeAll()
    static void setUp() {
        // Prevent the JVM from exiting when System.exit() is called
        System.setSecurityManager(new DoNotExitJVM());
    }

    @AfterAll
    static void tearDown() {
        // Restore the default security manager
        System.setSecurityManager(null);
    }

    @Test
    void cdk_5a7d75b_d500be0_3(@TempDir Path tempDir) throws Exception {
        // arrange
        Path projectDirectory = Paths.get("src/test/resources/it/cdk");
        assertThat("cdk submodule should exist", Files.exists(projectDirectory));

        Path classFilePath = Paths.get("base/standard/src/main/java/org/openscience/cdk/tools/HOSECodeGenerator.java");
        String selectedTests =
                "org.openscience.cdk.reaction.type.RadicalSiteHrBetaReactionTest#testInitiate_IAtomContainerSet_IAtomContainerSet";
        Path outputPath = tempDir.resolve("output.html");
        String[] args = {
            "-l",
            "5a7d75b",
            "-r",
            "d500be0",
            "-p",
            projectDirectory.toAbsolutePath().toString(),
            "-c",
            classFilePath.toString(),
            "--slug",
            "cdk/cdk",
            "--execution-depth",
            "3",
            "--selected-tests",
            selectedTests,
            "--output-path",
            outputPath.toAbsolutePath().toString(),
        };
        // act
        Main.main(args);
//        ExitException exit = assertThrows(ExitException.class, () -> Main.main(args));

        // assert
        assertThat("Exit code should be 0", 0, equalTo(0));

        Path expectedOutputPath = Paths.get("src/test/resources/it/resources/cdk_5a7d75b_d500be0_3.html");

        assertLinesMatch(Files.readAllLines(outputPath), Files.readAllLines(expectedOutputPath));
    }

    private static void assertLinesMatch(List<String> actual, List<String> expected) {
        // the following lines are not deterministic and should be ignored
        for (int i = 0; i < actual.size(); i++) {
            if (actual.get(i).contains("<meta name=\"request-id\"")) {
                continue;
            }
            if (actual.get(i).contains("<meta name=\"voltron-timing\"")) {
                continue;
            }
            if (actual.get(i)
                    .contains(
                            "<input type=\"hidden\" data-csrf=\"true\" class=\"js-data-jump-to-suggestions-path-csrf\"")) {
                continue;
            }
            if (actual.get(i)
                    .contains(
                            "<!-- '\"` --><!-- </textarea></xmp> --><form data-turbo=\"false\" action=\"/users/diffview\"")) {
                continue;
            }
            if (actual.get(i)
                    .contains(
                            "<div id=\"partial-timeline-marker\" class=\"js-timeline-marker js-socket-channel js-updatable-content\"")) {
                continue;
            }
            assertThat(actual.get(i) + ":: but was ::" + expected.get(i), actual.get(i), is(expected.get(i)));
        }
    }

    private static class DoNotExitJVM extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {}

        @Override
        public void checkPermission(Permission perm, Object context) {}

        /**
         * Throws an {@link ExitException} instead of exiting the JVM.
         *
         * @param status Exit code of the invocation
         */
        @Override
        public void checkExit(int status) {
            throw new ExitException(status);
        }
    }

    /**
     * A wrapper around the exit code of the JVM.
     * It prevents JVM from exiting and simply stores the exit code.
     */
    private static class ExitException extends SecurityException {
        public final int status;

        ExitException(int status) {
            this.status = status;
        }
    }
}
