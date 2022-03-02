# Collector Sahab

CLI to collect runtime context of a Java class.

### Execution

1. Build classpath.
    ```bash
   $ mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt
    ```
2. Compile files with debugging information attached.
    ```bash
   $ javac -g  -cp $(cat classpath.txt)  src/main/java/se/kth/debug/*.java -d target
    ```
3. Run the process.
    ```bash
   $ java -cp target/:$(cat classpath.txt) se.kth.debug.Runner
    ```

### Sample output
```bash
Mar 02, 2022 4:58:55 PM se.kth.debug.Debugger launchVMAndJunit
INFO: java -cp /home/assert/Desktop/testrepos/debugger/target:/home/assert/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar:/home/assert/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:/home/assert/Desktop/testrepos/debugger: se.kth.debug.MethodTestRunner se.kth.debug.AppTest#testAdd
Mar 02, 2022 4:58:55 PM se.kth.debug.Debugger launchVMAndJunit
INFO: Connected to port: 57505
Mar 02, 2022 4:58:55 PM se.kth.debug.Runner main
INFO: Classes are prepared!
Mar 02, 2022 4:58:55 PM se.kth.debug.Runner main
INFO: Breakpoint event is reached!
Mar 02, 2022 4:58:55 PM se.kth.debug.Debugger processBreakpoints
INFO: x::1
Mar 02, 2022 4:58:55 PM se.kth.debug.Debugger processBreakpoints
INFO: y::2
Mar 02, 2022 4:58:55 PM se.kth.debug.Runner main
WARNING: com.sun.jdi.VMDisconnectedException
```
