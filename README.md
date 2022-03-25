# Collector Sahab

CLI to collect runtime context of a Java class.

## Execution

### Setup project for collecting runtime statistics

The exact steps varies for each project because of multiple development
pipelines possible. However, the output from this step is same. In other
words, you need the following things to proceed to next step. It does not
matter how you get them.

1. Compiled classes of the project with debug (`-g`) set to `true`.
2. Compiled test classes and its resources bundled. Debugging can be on or off
   depending upon how much data you want.
3. Dependencies of the project as compiled classes. It is recommended to keep
   debugging off for this because you will get a lot of data otherwise!

There are two ways to achieve this:
1. Create JAR-with-dependencies of the project with test classes bundled too.
2. Use commands provided by maven plugins.
   1. `mvn compile`: for compiling classes
   2. `mvn test-compile`: for compiling test classes.
   3. `mvn dependency:copy-dependencies`: for copying dependencies in build folder.

### Build `collector-sahab`

1. Package the app
    ```bash
   $ mvn package
    ```
2. Prepare for execution of `collector sahab`.
   1. Create an `input` file containing a map of class names and breakpoints.
      Example:
      ```text
      ch.hsr.geohash.BoundingBox=210
      ch.hsr.geohash.util.DoubleUtil=12,15
      ```
   2. Run the process
      ```bash
      $ java -jar/target/debugger-1.0-SNAPSHOT-jar-with-dependencies.jar \
           -p [path/to/all/classes/required ...]
           -t [classname::testMethod ...]
           -i <path/to/input/file>
           -b <path/to/breakpoint/file>
           -r <path/to/return/file>
           --object-depth (default=1)
      ```
