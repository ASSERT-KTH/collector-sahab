# Collector Sahab

CLI to collect runtime context of a Java class.

## Execution

### Setup project for collecting runtime statistics
1. Build classpath of the project you want to collect runtime information of.
    ```bash
   $ mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt
    ```
2. Build the project with debugging information attached.
   1. Since `javac` does not compile recursively through directories, we need
      to create a list of files we want to compile.
      ```bash
      $ find src/ -name "*.java" > sources.txt
      ```
   2. Compile the project by running the following command.
      ```bash
      $ javac -g -cp $(cat classpath.txt) @sources.txt -d target
      ```
      > NOTE: Test resources are not compiled as of now.

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
           -p </path/to/project>
           -t </path/to/project/test/directory>
           -i <path/to/input/file> (default="input.txt")
      ```
