# Collector Sahab

[![GHA tests Workflow Status](https://github.com/algomaster99/collector-sahab/actions/workflows/tests.yml/badge.svg)](https://github.com/algomaster99/collector-sahab/actions/workflows/tests.yml)

CLI to collect runtime context of a Java class.

## Structure of the project

The project is divided into multiple submodules.
1. `common`: Contains common classes used by other modules.
1. `collector-sahab`: The main module which contains the CLI and runs all the following modules.
1. `matched-line-finder`: A submodule which is used to find the _non-diffed_ line numbers
   in the left and right commits.
1. `trace-collector`: A submodule which is a java agent which collects runtime data
   and writes it to a file.
1. `trace-diff`: A submodule which is used to find the diff between the runtime data
   collected by `trace-collector` in the left and right commits.


## Execution

### Prerequisites

   Please install `gawk` before proceeding. Use the following command.
```shell
$ sudo apt install gawk
```

### Setup project for collecting runtime statistics

1. Package the entire project
    ```bash
   $ mvn package -DskipTests
    ```
2. You will get the agent at this path `trace-collector/target/trace-collector.jar`.
   Copy this anywhere in the system and paste its path in `PomTransformer.AGENT_JAR`.
   > See issue [#149](https://github.com/ASSERT-KTH/collector-sahab/issues/149). 

3. Package the project again so that the path gets registered.
    ```bash
   $ mvn package -DskipTests
    ```
4. Prepare for execution of `collector sahab` by running. The following are the required parameters.
   ```bash
   $ java -jar main/target/collector-sahab-1.0-SNAPSHOT-jar-with-dependencies.jar \
          -p <path/to/project>
          -l <left-commit>
          -r <right-commit>
          -c <relative/path/to/classfile>
          --slug <orgName/repoName>
   ```
   The following are the optional parameters.
   1. `--execution-depth` (integer): The depth of the stack trace to be collected. Default is `0`.
   1. `--selected-test` (list of tests separated by comma): The tests to be executed. Default is `[]` which
      runs every test in the target project.
   2. `--output-path` (string): The path where the output will be stored. Default is `output.html`.

   Following parameters have not been added to `main`, but planned to be added in the future if needed.
   
   1.  `numberOfArrayElements`: The number of array elements to be collected. Default is `10`.

   2. `extractParameters`: Whether to extract parameters of the method. Default is `false`. 


## Example trace

 ```json
{
   "breakpoint": [
         {
            "file": "foo/BasicMath.java",
            "lineNumber": 5,
            "stackFrameContexts": [
            {
               "positionFromTopInStackTrace": 1,
               "location": "foo.BasicMath:5",
               "stackTrace": [
                  "add:5, foo.BasicMath",
                  "test_add:11, foo.BasicMathTest",
                  "runTest:40, se.kth.debug.JUnitTestRunner",
                  "lambda$main$0:16, se.kth.debug.JUnitTestRunner",
                  "call:-1, se.kth.debug.JUnitTestRunner$$Lambda$1.81628611"
               ],
               "runtimeValueCollection": [
                  {
                     "kind": "LOCAL_VARIABLE",
                     "name": "x",
                     "type": "int",
                     "value": 23,
                     "fields": null,
                     "arrayElements": null
                  },
                  {  
                     "kind": "LOCAL_VARIABLE",
                     "name": "y",
                     "type": "int",
                     "value": 2,
                     "fields": null,
                     "arrayElements": null
                  }
               ]
            }
         ]
      },
      {
         "file": "foo/BasicMath.java",
         "lineNumber": 9,
         "stackFrameContexts": [
            {
               "positionFromTopInStackTrace": 1,
               "location": "foo.BasicMath:9",
               "stackTrace": [
                  "subtract:9, foo.BasicMath",
                  "test_subtract:16, foo.BasicMathTest",
                  "runTest:40, se.kth.debug.JUnitTestRunner",
                  "lambda$main$0:16, se.kth.debug.JUnitTestRunner",
                  "call:-1, se.kth.debug.JUnitTestRunner$$Lambda$1.81628611"
               ],
               "runtimeValueCollection": [
                  {
                     "kind": "LOCAL_VARIABLE",
                     "name": "x",
                     "type": "int",
                     "value": 2,
                     "fields": null,
                     "arrayElements": null
                  },
                  {
                     "kind": "LOCAL_VARIABLE",
                     "name": "y",
                     "type": "int",
                     "value": 1,
                     "fields": null,
                     "arrayElements": null
                  } 
               ]
            }
         ]
      }
   ]
}
```

