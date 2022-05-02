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
           -i <path/to/input/breakpoint/file>
           -m <path/to/input/return/file>
           -o <path/to/output>
           --execution-depth (default=0)
           --number-of-array-elements (default=10)
           --skip-printing-field (default=false)
           --stack-trace-depth (default=0)
      ```

## Scripts

### MatchedLineFinder

It takes in exactly four arguments in the specified order:
1. **Absolute path to project** whose runtime data needs to be collected.
2. **Filename** of the class where the patch is.
3. **Left** commit, or any reference acceptable by `git checkout`.
4. **Right** commit, or any reference acceptable by `git checkout`.

> "or any reference acceptable by `git checkout`" is not tested for.

**Example execution**
```bash
java -cp target/debugger-1.0-SNAPSHOT-jar-with-dependencies.jar se.kth.debug.MatchedLineFinder /home/assert/Desktop/experiments/drr-as-pr/Time-11 DateTimeZoneBuilder.java e5d67a8162aebb7dbd5df8cdc21442ef111d2ba1 1c04679173a46faa59e73f68def33f60843f8beb
```

> Runs as a part of `script/bribe-sahab.py`

### scripts/bribe-sahab.py

It does three things:
1. Compiles target project for both revisions
2. Find the matched lines
3. Run collector-sahab over the project to collect runtime context

```bash
usage: Bribe sahab [-h] -p PROJECT -l LEFT -r RIGHT -c CLASS_FILENAME -t TESTS [TESTS ...]

optional arguments:
  -h, --help            show this help message and exit
  -p PROJECT, --project PROJECT
                        Path to project
  -l LEFT, --left LEFT  Left revision
  -r RIGHT, --right RIGHT
                        Right revision
  -c CLASS_FILENAME, --class-filename CLASS_FILENAME
                        Name of the file which contains the patch
  -t TESTS [TESTS ...], --tests TESTS [TESTS ...]
                        Tests executed by collector sahab
```
