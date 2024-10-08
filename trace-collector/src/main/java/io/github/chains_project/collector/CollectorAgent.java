package io.github.chains_project.collector;

import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chains_project.collector.module.ModuleCracker;
import io.github.chains_project.collector.util.ByteBuddyHelper;
import io.github.chains_project.collector.util.ContextCollector;
import io.github.chains_project.cs.commons.CollectorAgentOptions;
import io.github.chains_project.cs.commons.FileAndBreakpoint;
import io.github.chains_project.cs.commons.MethodForExitEvent;
import io.github.chains_project.cs.commons.runtime.LocalVariable;
import io.github.chains_project.cs.commons.util.Classes;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.description.type.TypeDescription.Generic.OfNonGenericType.ForLoadedType;
import net.bytebuddy.description.type.TypeDescription.Latent;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.constant.ClassConstant;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.util.TraceClassVisitor;

public class CollectorAgent {

    public static ModuleCracker moduleCracker;
    private static CollectorAgentOptions options;

    // Needed to store the array of parameter values we create during method entry
    private static final int STACK_OFFSET = 42;

    public static void premain(String arguments, Instrumentation instrumentation) {
        moduleCracker = ModuleCracker.getApplicable(instrumentation);
        options = new CollectorAgentOptions(arguments);
        ContextCollector.setExecutionDepth(options.getExecutionDepth());
        List<String> classesAllowed = getClassesAllowed();
        List<String> methodExits = getMethodExits();
        instrumentation.addTransformer(
                new ClassFileTransformer() {
                    @Override
                    public byte[] transform(
                            ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
                        try {
                            return getBytes(className, classfileBuffer, classesAllowed, methodExits);
                        } catch (Throwable t) {
                            t.printStackTrace();
                            throw new RuntimeException(t);
                        }
                    }
                },
                true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                new ObjectMapper()
                        .writer(new DefaultPrettyPrinter().withArrayIndenter(new DefaultIndenter("  ", "\n")))
                        .writeValue(options.getOutput(), ContextCollector.getSahabOutput());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    private static List<String> getClassesAllowed() {
        List<FileAndBreakpoint> fileAndBreakpoints = options.getClassesAndBreakpoints();
        List<MethodForExitEvent> methodForExitEvents = options.getMethodsForExitEvent();

        return Stream.concat(
                        fileAndBreakpoints.stream().map(FileAndBreakpoint::getFileName),
                        methodForExitEvents.stream().map(MethodForExitEvent::getClassName))
                .collect(Collectors.toList());
    }

    private static List<Integer> getBreakpointsAllowed(String className) {
        List<FileAndBreakpoint> fileAndBreakpoints = options.getClassesAndBreakpoints();
        for (FileAndBreakpoint fileAndBreakpoint : fileAndBreakpoints) {
            if (fileAndBreakpoint.getFileName().equals(className)) {
                return fileAndBreakpoint.getBreakpoints();
            }
        }
        return new ArrayList<>();
    }

    private static List<String> getMethodExits() {
        List<MethodForExitEvent> methodsForExitEvents = options.getMethodsForExitEvent();
        return methodsForExitEvents.stream().map(MethodForExitEvent::getName).collect(Collectors.toList());
    }

    private static byte[] getBytes(
            String className, byte[] classfileBuffer, List<String> classesAllowed, List<String> methodExits)
            throws NoSuchMethodException {
        if (!classesAllowed.contains(className)) {
            return classfileBuffer;
        }
        List<Integer> breakpointsAllowed = getBreakpointsAllowed(className);

        ClassNode classNode = generateClassNode(classfileBuffer);

        processClassMethods(methodExits, classNode, breakpointsAllowed, className);

        return getTransformedClass(classNode);
    }

    private static ClassNode generateClassNode(byte[] classfileBuffer) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(classfileBuffer);
        classReader.accept(classNode, 0);
        return classNode;
    }

    private static void processClassMethods(
            List<String> methodExits, ClassNode classNode, List<Integer> breakpointsAllowed, String className)
            throws NoSuchMethodException {
        for (MethodNode method : classNode.methods) {
            List<LocalVariableNode> liveVariables = new ArrayList<>();
            processInstructions(methodExits, classNode, breakpointsAllowed, className, method, liveVariables);
        }
    }

    private static void processInstructions(
            List<String> methodExits,
            ClassNode classNode,
            List<Integer> breakpointsAllowed,
            String className,
            MethodNode method,
            List<LocalVariableNode> liveVariables)
            throws NoSuchMethodException {
        for (AbstractInsnNode instruction : method.instructions) {
            processInstruction(
                    methodExits, classNode, breakpointsAllowed, className, method, liveVariables, instruction);
        }
    }

    private static void processInstruction(
            List<String> methodExits,
            ClassNode classNode,
            List<Integer> breakpointsAllowed,
            String className,
            MethodNode method,
            List<LocalVariableNode> liveVariables,
            AbstractInsnNode instruction)
            throws NoSuchMethodException {
        AbstractInsnNode currentNode = instruction;

        if (shouldMethodExitBeRecorded(method, methodExits) && instruction.getPrevious() == null) {
            currentNode = insertCallToEntryLog(method, currentNode);
        }

        if (instruction instanceof LabelNode) {
            processLabelNode(method, liveVariables, currentNode);
        }

        if (instruction instanceof LineNumberNode && breakpointsAllowed.contains(((LineNumberNode) instruction).line)) {
            currentNode = insertCallToLineLog(className, method, liveVariables, currentNode);
        }

        if (shouldMethodExitBeRecorded(method, methodExits) && isItExitInstruction(instruction.getOpcode())) {
            currentNode = insertCallToReturnLog(className, method, currentNode);
        }
    }

    private static void processLabelNode(
            MethodNode method, List<LocalVariableNode> liveVariables, AbstractInsnNode currentNode) {
        for (LocalVariableNode localVariable : method.localVariables) {
            if (localVariable.start == currentNode) {
                liveVariables.add(localVariable);
            }
            if (localVariable.end == currentNode) {
                liveVariables.remove(localVariable);
            }
        }
    }

    private static AbstractInsnNode insertCallToEntryLog(MethodNode method, AbstractInsnNode currentNode)
            throws NoSuchMethodException {
        StackManipulation callEntryLog = getCallToEntryLogMethod(method);
        return ByteBuddyHelper.applyStackManipulation(
                method, currentNode, callEntryLog, ByteBuddyHelper.InsertPosition.BEFORE);
    }

    private static AbstractInsnNode insertCallToLineLog(
            String className, MethodNode method, List<LocalVariableNode> liveVariables, AbstractInsnNode currentNode)
            throws NoSuchMethodException {
        StackManipulation callLineLog =
                getCallToLineLogMethod(className, method, liveVariables, (LineNumberNode) currentNode);
        return ByteBuddyHelper.applyStackManipulation(
                method, currentNode, callLineLog, ByteBuddyHelper.InsertPosition.AFTER);
    }

    private static AbstractInsnNode insertCallToReturnLog(
            String className, MethodNode method, AbstractInsnNode currentNode) throws NoSuchMethodException {
        StackManipulation callReturnLog = getCallToReturnLogMethod(className, method);
        return ByteBuddyHelper.applyStackManipulation(
                method, currentNode, callReturnLog, ByteBuddyHelper.InsertPosition.BEFORE);
    }

    private static byte[] getTransformedClass(ClassNode classNode) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(new TraceClassVisitor(writer, null));
        return writer.toByteArray();
    }

    private static boolean shouldMethodExitBeRecorded(MethodNode method, List<String> methodExits) {
        return methodExits.contains(method.name);
    }

    private static boolean isItExitInstruction(int opCode) {
        return (opCode >= IRETURN && opCode <= RETURN);
    }

    private static StackManipulation.Compound getCallToEntryLogMethod(MethodNode method) throws NoSuchMethodException {
        List<StackManipulation> manipulations = new ArrayList<>();
        List<StackManipulation> arguments = new ArrayList<>();

        if (options.getExtractParameters()) {
            Type[] parameterTypes = Type.getArgumentTypes(method.desc);
            for (int i = 0; i < parameterTypes.length; i++) {
                ParameterNode parameterNode = method.parameters.get(i);
                int readIndex = 0;
                for (LocalVariableNode localVariable : method.localVariables) {
                    if (localVariable.name.equals(parameterNode.name)) {
                        readIndex = localVariable.index;
                    }
                }
                TypeDescription type = getLatentType(parameterTypes[i]);
                arguments.add(createLocalVariable(method.parameters.get(i).name, readIndex, type));
            }
        }

        ArrayFactory arrayFactory = ArrayFactory.forType(ForLoadedType.of(LocalVariable.class));
        manipulations.add(arrayFactory.withValues(arguments));

        // Convert them to freeze their values
        manipulations.add(MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(
                ContextCollector.class.getMethod("convertLocalVariables", LocalVariable[].class))));

        manipulations.add(MethodVariableAccess.of(ForLoadedType.of(List.class))
                .storeAt(indexOfLastLocalVariable(method) + STACK_OFFSET));

        return new StackManipulation.Compound(manipulations);
    }

    private static TypeDescription getLatentType(Type type) {
        // Arrays can only be looked up using their internal-ish name:
        // "int[]" does not work, "[I" does.
        // "java.lang.String[]" does not work, "[Ljava.lamg.String;" does.
        if (type.getInternalName().startsWith("[")) {
            return getLatentType(type.getInternalName().replace("/", "."));
        }
        return getLatentType(type.getClassName());
    }

    private static TypeDescription getLatentType(String name) {
        Class<?> primitive = Classes.getPrimitiveFromString(name);
        if (primitive != null) {
            return TypeDescription.ForLoadedType.of(primitive);
        }
        return new Latent(name, 0, null);
    }

    private static StackManipulation getCallToLineLogMethod(
            String className, MethodNode method, List<LocalVariableNode> liveVariables, LineNumberNode node)
            throws NoSuchMethodException {
        List<StackManipulation> manipulations = new ArrayList<>();

        List<StackManipulation> values = new ArrayList<>();
        for (LocalVariableNode liveVariable : liveVariables) {
            // Skip "this" object
            if (liveVariable.index == 0 && !Modifier.isStatic(method.access)) {
                continue;
            }
            TypeDescription variableType = getLatentType(Type.getType(liveVariable.desc));
            values.add(createLocalVariable(liveVariable.name, liveVariable.index, variableType));
        }

        // Stack is still empty.

        //  public static void logLine(
        //    String file,
        manipulations.add(new TextConstant(className));
        //    int lineNumber,
        manipulations.add(IntegerConstant.forValue(node.line));
        //    Object receiver,
        if (Modifier.isStatic(method.access)) {
            manipulations.add(NullConstant.INSTANCE);
        } else {
            manipulations.add(MethodVariableAccess.loadThis());
        }
        //    LocalVariable[] localVariables
        manipulations.add(
                ArrayFactory.forType(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(LocalVariable.class))
                        .withValues(values));
        //    Class<?> receiverClass
        manipulations.add(ClassConstant.of(getLatentType(className.replace('/', '.'))));
        //  );

        manipulations.add(
                MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(ContextCollector.class.getMethod(
                        "logLine", String.class, int.class, Object.class, LocalVariable[].class, Class.class))));

        return new StackManipulation.Compound(manipulations);
    }

    private static StackManipulation.Compound createLocalVariable(String name, int readIndex, TypeDescription type)
            throws NoSuchMethodException {
        return new StackManipulation.Compound(List.of(
                // new LocalVariable(
                TypeCreation.of(TypeDescription.ForLoadedType.of(LocalVariable.class)),
                Duplication.of(TypeDescription.ForLoadedType.of(LocalVariable.class)),
                //   String name
                new TextConstant(name),
                // , Class<?> type
                ClassConstant.of(type),
                // , Object value
                MethodVariableAccess.of(type).loadFrom(readIndex),
                Assigner.GENERICS_AWARE.assign(
                        type.asGenericType(), ForLoadedType.of(Object.class), Assigner.Typing.STATIC),
                // );
                MethodInvocation.invoke(new MethodDescription.ForLoadedConstructor(
                        LocalVariable.class.getConstructor(String.class, Class.class, Object.class)))));
    }

    private static StackManipulation getCallToReturnLogMethod(String className, MethodNode method)
            throws NoSuchMethodException {
        List<StackManipulation> manipulations = new ArrayList<>();

        // Stack is empty.

        // public static void logReturn(
        //   Object returnValue,
        Generic typeDesc =
                getLatentType(Type.getMethodType(method.desc).getReturnType()).asGenericType();
        manipulations.add(Duplication.of(typeDesc));
        manipulations.add(
                Assigner.GENERICS_AWARE.assign(typeDesc, ForLoadedType.of(Object.class), Assigner.Typing.DYNAMIC));
        //   String methodName,
        manipulations.add(new TextConstant(method.name));
        //   Class<?> returnTypeName
        manipulations.add(
                ClassConstant.of(getLatentType(Type.getMethodType(method.desc).getReturnType())));

        //   Class<?> className
        manipulations.add(ClassConstant.of(getLatentType(className.replace('/', '.'))));

        //   LocalVariable[] parameterValues
        manipulations.add(MethodVariableAccess.of(ForLoadedType.of(LocalVariable[].class))
                .loadFrom(indexOfLastLocalVariable(method) + STACK_OFFSET));

        manipulations.add(
                MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(ContextCollector.class.getMethod(
                        "logReturn", Object.class, String.class, Class.class, Class.class, List.class))));

        return new StackManipulation.Compound(manipulations);
    }

    private static int indexOfLastLocalVariable(MethodNode method) {
        int max = 0;
        for (LocalVariableNode localVariable : method.localVariables) {
            max = Math.max(max, localVariable.index);
        }
        return max;
    }
}
