package se.assertteam;

import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.description.type.TypeDescription.Generic.OfNonGenericType.ForLoadedType;
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
import org.objectweb.asm.util.TraceClassVisitor;
import se.assertteam.module.ModuleCracker;
import se.kth.debug.struct.FileAndBreakpoint;
import se.kth.debug.struct.MethodForExitEvent;

public class CollectorAgent {

    public static ModuleCracker moduleCracker;
    private static CollectorAgentOptions options;

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
        List<String> classesAllowed = new ArrayList<>();
        classesAllowed.addAll(
                fileAndBreakpoints.stream().map(FileAndBreakpoint::getFileName).collect(Collectors.toList()));
        classesAllowed.addAll(methodForExitEvents.stream()
                .map(MethodForExitEvent::getClassName)
                .collect(Collectors.toList()));
        return classesAllowed;
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

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(classfileBuffer);
        classReader.accept(classNode, 0);

        for (MethodNode method : classNode.methods) {
            List<LocalVariableNode> liveVariables = new ArrayList<>();
            for (AbstractInsnNode instruction : method.instructions) {
                AbstractInsnNode currentNode = instruction;

                if (instruction instanceof LabelNode) {
                    for (LocalVariableNode localVariable : method.localVariables) {
                        if (localVariable.start == currentNode) {
                            liveVariables.add(localVariable);
                        }
                    }
                }
                if (instruction instanceof LineNumberNode) {
                    if (breakpointsAllowed.contains(((LineNumberNode) instruction).line)) {
                        StackManipulation callLineLog =
                                getCallToLineLogMethod(className, method, liveVariables, (LineNumberNode) instruction);
                        currentNode = ByteBuddyHelper.applyStackManipulation(
                                method, currentNode, callLineLog, ByteBuddyHelper.InsertPosition.AFTER);
                    }
                }

                int opCode = instruction.getOpcode();
                if (shouldMethodExitBeRecorded(method, methodExits) && isItExitInstruction(opCode)) {
                    StackManipulation callReturnLog = getCallToReturnLogMethod(className, method);
                    currentNode = ByteBuddyHelper.applyStackManipulation(
                            method, currentNode, callReturnLog, ByteBuddyHelper.InsertPosition.BEFORE);
                }
            }
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(new TraceClassVisitor(writer, null));
        return writer.toByteArray();
    }

    private static boolean shouldMethodExitBeRecorded(MethodNode method, List<String> methodExits) {
        return methodExits.contains(method.name);
    }

    private static boolean isItExitInstruction(int opCode) {
        return (opCode >= IRETURN && opCode <= RETURN) || opCode == ATHROW;
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
            Class<?> type =
                    Classes.getClassFromString(Type.getType(liveVariable.desc).getClassName());
            values.add(createLocalVariable(liveVariable.name, liveVariable.index, type));
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
        manipulations.add(new TextConstant(className.replace('/', '.')));
        //  );

        manipulations.add(
                MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(ContextCollector.class.getMethod(
                        "logLine", String.class, int.class, Object.class, LocalVariable[].class, String.class))));

        return new StackManipulation.Compound(manipulations);
    }

    private static StackManipulation.Compound createLocalVariable(String name, int readIndex, Class<?> type)
            throws NoSuchMethodException {
        return new StackManipulation.Compound(List.of(
                // new LocalVariable(
                TypeCreation.of(TypeDescription.ForLoadedType.of(LocalVariable.class)),
                Duplication.of(TypeDescription.ForLoadedType.of(LocalVariable.class)),
                //   String name
                new TextConstant(name),
                // , Class<?> type
                ClassConstant.of(TypeDescription.ForLoadedType.of(type)),
                // , Object value
                MethodVariableAccess.of(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(type))
                        .loadFrom(readIndex),
                Assigner.GENERICS_AWARE.assign(
                        TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(type),
                        TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class),
                        Assigner.Typing.STATIC),
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
        Generic typeDesc = ForLoadedType.of(Classes.getClassFromString(
                Type.getMethodType(method.desc).getReturnType().getClassName()));
        manipulations.add(Duplication.of(typeDesc));
        manipulations.add(
                Assigner.GENERICS_AWARE.assign(typeDesc, ForLoadedType.of(Object.class), Assigner.Typing.DYNAMIC));
        //   String methodName,
        manipulations.add(new TextConstant(method.name));
        //   String returnTypeName
        manipulations.add(
                new TextConstant(Type.getMethodType(method.desc).getReturnType().getClassName()));

        //   String className
        manipulations.add(new TextConstant(className.replace('/', '.')));
        //  );
        manipulations.add(
                MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(ContextCollector.class.getMethod(
                        "logReturn", Object.class, String.class, String.class, String.class))));

        return new StackManipulation.Compound(manipulations);
    }
}