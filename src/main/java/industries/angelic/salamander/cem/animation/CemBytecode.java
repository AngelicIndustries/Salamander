package industries.angelic.salamander.cem.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Compiles a model's animation program to one JVM class. Each expression tree becomes straight-line code over
 * {@link CemExpression.Context}'s slot arrays instead of a chain of closure calls. Comparisons and functions call
 * {@link CemFunctions}, as the interpreter does, so both give bit-identical results.
 */
public final class CemBytecode implements Opcodes {

    /** One {@code target = expression} of a model, its target already resolved to a slot. */
    public static final class Assignment {

        public final int kind;
        public final int index;
        public final boolean bool;
        public final CemSyntax expression;

        public Assignment(int kind, int index, boolean bool, CemSyntax expression) {
            this.kind = kind;
            this.index = index;
            this.bool = bool;
            this.expression = expression;
        }
    }

    private static final String CONTEXT = Type.getInternalName(CemExpression.Context.class);
    private static final String RUN = "(L" + CONTEXT + ";)V";
    private static final String EXPRESSION = Type.getInternalName(CemExpression.class);
    private static final String FALLBACKS = "[L" + EXPRESSION + ";";
    private static final String PROGRAM = Type.getInternalName(CemProgram.class);
    private static final String FUNCTIONS = Type.getInternalName(CemFunctions.class);
    private static final String VARIABLES = Type.getInternalName(CemVariables.class);
    /** HotSpot does not JIT-compile methods over 8000 bytes, so the program is split into methods under this. */
    private static final int METHOD_BYTES = 6000;
    private static final AtomicInteger CLASSES = new AtomicInteger();

    private CemBytecode() {}

    /** {@code name} only labels the generated class, which shows in profilers and stack traces. */
    public static CemProgram compile(String name, List<Assignment> program) throws ReflectiveOperationException {
        String stem = name.replaceAll(".*[/:]", "")
            .replaceAll("\\..*", "")
            .replaceAll("[^A-Za-z0-9_]", "_");
        String className = "industries/angelic/salamander/cem/generated/" + (stem.isEmpty() ? "model" : stem)
            + "$"
            + CLASSES.incrementAndGet();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {

            @Override
            protected String getCommonSuperClass(String first, String second) {
                return "java/lang/Object";
            }
        };
        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER | ACC_SYNTHETIC, className, null, PROGRAM, null);
        MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", "(" + FALLBACKS + ")V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitVarInsn(ALOAD, 1);
        init.visitMethodInsn(INVOKESPECIAL, PROGRAM, "<init>", "(" + FALLBACKS + ")V", false);
        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        List<CemExpression> fallbacks = new ArrayList<>();
        int methods = 0;
        for (int next = 0; next < program.size(); methods++) {
            MethodVisitor method = writer.visitMethod(ACC_PRIVATE, "run" + methods, RUN, null, null);
            method.visitCode();
            Emitter emitter = new Emitter(method, fallbacks);
            do {
                emitter.assignment(program.get(next++));
            } while (next < program.size() && emitter.size + size(program.get(next)) <= METHOD_BYTES);
            method.visitInsn(RETURN);
            method.visitMaxs(0, 0);
            method.visitEnd();
        }
        MethodVisitor run = writer.visitMethod(ACC_PUBLIC, "run", RUN, null, null);
        run.visitCode();
        for (int i = 0; i < methods; i++) {
            run.visitVarInsn(ALOAD, 0);
            run.visitVarInsn(ALOAD, 1);
            run.visitMethodInsn(INVOKESPECIAL, className, "run" + i, RUN, false);
        }
        run.visitInsn(RETURN);
        run.visitMaxs(0, 0);
        run.visitEnd();
        writer.visitEnd();

        Class<?> type = new Loader(CemProgram.class.getClassLoader())
            .define(className.replace('/', '.'), writer.toByteArray());
        return (CemProgram) type.getConstructor(CemExpression[].class)
            .newInstance((Object) fallbacks.toArray(new CemExpression[0]));
    }

    /** The bytes an assignment will take, measured by emitting it nowhere. */
    private static int size(Assignment assignment) {
        Emitter emitter = new Emitter(new MethodVisitor(ASM5) {}, new ArrayList<>());
        emitter.assignment(assignment);
        return emitter.size;
    }

    /** Emits into one method, keeping a close estimate of the bytecode size so far. */
    private static final class Emitter {

        final MethodVisitor method;
        final List<CemExpression> fallbacks;
        int size;
        /** 0 is the program, 1 the context. */
        int locals = 2;

        Emitter(MethodVisitor method, List<CemExpression> fallbacks) {
            this.method = method;
            this.fallbacks = fallbacks;
        }

        void assignment(Assignment assignment) {
            switch (assignment.kind) {
                case CemSlot.POSE:
                    array("pose");
                    index(assignment.index);
                    value(assignment);
                    insn(DASTORE);
                    break;
                case CemSlot.RENDER:
                    array("renderValues");
                    index(assignment.index);
                    value(assignment);
                    insn(DASTORE);
                    break;
                case CemSlot.VARIABLE:
                    context();
                    field("slots", "L" + VARIABLES + ";");
                    index(assignment.index);
                    value(assignment);
                    invoke(INVOKEVIRTUAL, VARIABLES, "set", "(ID)V");
                    break;
                default:
                    throw new IllegalArgumentException("CEM inputs are read-only");
            }
        }

        private void value(Assignment assignment) {
            expression(assignment.expression);
            if (assignment.bool) function("bool", "(D)D");
        }

        /** Leaves the expression's double on the operand stack. */
        void expression(CemSyntax node) {
            if (node instanceof CemSyntax.Constant) {
                method.visitLdcInsn(((CemSyntax.Constant) node).value);
                size += 3;
            } else if (node instanceof CemSyntax.Reference) {
                CemExpression value = ((CemSyntax.Reference) node).value;
                if (value instanceof CemSlot) slot((CemSlot) value);
                else fallback(value);
            } else if (node instanceof CemSyntax.Input) {
                context();
                field("inputs", "Ljava/util/Map;");
                method.visitLdcInsn(((CemSyntax.Input) node).name);
                size += 3;
                function("input", "(Ljava/util/Map;Ljava/lang/String;)D");
            } else if (node instanceof CemSyntax.Unary) {
                CemSyntax.Unary unary = (CemSyntax.Unary) node;
                expression(unary.operand);
                if (unary.operator == '!') function("not", "(D)D");
                else insn(DNEG);
            } else if (node instanceof CemSyntax.Binary) binary((CemSyntax.Binary) node);
            else if (node instanceof CemSyntax.Call) call((CemSyntax.Call) node);
            else fallback(node.interpret());
        }

        private void binary(CemSyntax.Binary node) {
            switch (node.operator) {
                case "||":
                case "&&": {
                    boolean or = node.operator.equals("||");
                    Label decided = new Label(), end = new Label();
                    expression(node.left);
                    truth();
                    jump(or ? IFNE : IFEQ, decided);
                    expression(node.right);
                    truth();
                    jump(or ? IFNE : IFEQ, decided);
                    insn(or ? DCONST_0 : DCONST_1);
                    jump(GOTO, end);
                    method.visitLabel(decided);
                    insn(or ? DCONST_1 : DCONST_0);
                    method.visitLabel(end);
                    return;
                }
                case "+":
                    arithmetic(node, DADD);
                    return;
                case "-":
                    arithmetic(node, DSUB);
                    return;
                case "*":
                    arithmetic(node, DMUL);
                    return;
                case "/":
                    arithmetic(node, DDIV);
                    return;
                case "%":
                    arithmetic(node, DREM);
                    return;
                case "==":
                    compare(node, "eq");
                    return;
                case "!=":
                    compare(node, "ne");
                    return;
                case ">=":
                    compare(node, "ge");
                    return;
                case "<=":
                    compare(node, "le");
                    return;
                case ">":
                    compare(node, "gt");
                    return;
                case "<":
                    compare(node, "lt");
                    return;
                default:
                    fallback(node.interpret());
            }
        }

        private void arithmetic(CemSyntax.Binary node, int opcode) {
            expression(node.left);
            expression(node.right);
            insn(opcode);
        }

        private void compare(CemSyntax.Binary node, String function) {
            expression(node.left);
            expression(node.right);
            function(function, "(DD)D");
        }

        private void call(CemSyntax.Call node) {
            CemSyntax[] args = node.arguments;
            switch (node.name) {
                case "if":
                case "ifb": {
                    Label end = new Label();
                    for (int i = 0; i < args.length - 1; i += 2) {
                        Label next = new Label();
                        expression(args[i]);
                        truth();
                        jump(IFEQ, next);
                        expression(args[i + 1]);
                        jump(GOTO, end);
                        method.visitLabel(next);
                    }
                    expression(args[args.length - 1]);
                    method.visitLabel(end);
                    return;
                }
                case "min":
                case "max":
                    expression(args[0]);
                    for (int i = 1; i < args.length; i++) {
                        expression(args[i]);
                        invoke(INVOKESTATIC, "java/lang/Math", node.name, "(DD)D");
                    }
                    return;
                case "random":
                    if (args.length == 0) function("random", "()D");
                    else {
                        expression(args[0]);
                        function("random", "(D)D");
                    }
                    return;
                case "in": {
                    int value = locals++;
                    Label match = new Label(), end = new Label();
                    expression(args[0]);
                    insn(D2F);
                    local(FSTORE, value);
                    for (int i = 1; i < args.length; i++) {
                        local(FLOAD, value);
                        expression(args[i]);
                        insn(D2F);
                        insn(FCMPL);
                        jump(IFEQ, match);
                    }
                    insn(DCONST_0);
                    jump(GOTO, end);
                    method.visitLabel(match);
                    insn(DCONST_1);
                    method.visitLabel(end);
                    return;
                }
                case "print":
                case "printb":
                    fallback(node.interpret());
                    return;
                default:
                    break;
            }
            for (CemSyntax arg : args) expression(arg);
            switch (node.name) {
                case "sin":
                case "cos":
                case "tan":
                case "asin":
                case "acos":
                case "atan":
                case "abs":
                case "floor":
                case "ceil":
                case "sqrt":
                case "exp":
                case "log":
                case "signum":
                    invoke(INVOKESTATIC, "java/lang/Math", node.name, "(D)D");
                    return;
                case "torad":
                    invoke(INVOKESTATIC, "java/lang/Math", "toRadians", "(D)D");
                    return;
                case "todeg":
                    invoke(INVOKESTATIC, "java/lang/Math", "toDegrees", "(D)D");
                    return;
                case "atan2":
                case "pow":
                    invoke(INVOKESTATIC, "java/lang/Math", node.name, "(DD)D");
                    return;
                case "round":
                case "frac":
                    function(node.name, "(D)D");
                    return;
                case "wraprad":
                    function("wrapRad", "(D)D");
                    return;
                case "wrapdeg":
                    function("wrapDeg", "(D)D");
                    return;
                case "fmod":
                    function("fmod", "(DD)D");
                    return;
                case "clamp":
                case "between":
                case "equals":
                case "lerp":
                    function(node.name, "(DDD)D");
                    return;
                default:
                    throw new IllegalStateException("No bytecode for CEM function " + node.name);
            }
        }

        private void slot(CemSlot slot) {
            switch (slot.kind) {
                case CemSlot.POSE:
                    array("pose");
                    index(slot.index);
                    insn(DALOAD);
                    return;
                case CemSlot.INPUT:
                    array("inputValues");
                    index(slot.index);
                    insn(DALOAD);
                    return;
                case CemSlot.RENDER:
                    array("renderValues");
                    index(slot.index);
                    insn(DALOAD);
                    return;
                case CemSlot.VARIABLE:
                    context();
                    field("slots", "L" + VARIABLES + ";");
                    index(slot.index);
                    invoke(INVOKEVIRTUAL, VARIABLES, "get", "(I)D");
                    return;
                default:
                    fallback(slot);
            }
        }

        /** Calls an interpreted expression held by the program. */
        private void fallback(CemExpression expression) {
            int index = fallbacks.size();
            fallbacks.add(expression);
            method.visitVarInsn(ALOAD, 0);
            method.visitFieldInsn(GETFIELD, PROGRAM, "fallbacks", FALLBACKS);
            size += 4;
            index(index);
            insn(AALOAD);
            context();
            method.visitMethodInsn(INVOKEINTERFACE, EXPRESSION, "evaluate", "(L" + CONTEXT + ";)D", true);
            size += 5;
        }

        private void context() {
            method.visitVarInsn(ALOAD, 1);
            size += 1;
        }

        private void array(String name) {
            context();
            field(name, "[D");
        }

        private void field(String name, String descriptor) {
            method.visitFieldInsn(GETFIELD, CONTEXT, name, descriptor);
            size += 3;
        }

        private void index(int value) {
            if (value <= Short.MAX_VALUE) method.visitIntInsn(SIPUSH, value);
            else method.visitLdcInsn(value);
            size += 3;
        }

        private void insn(int opcode) {
            method.visitInsn(opcode);
            size += 1;
        }

        private void local(int opcode, int index) {
            method.visitVarInsn(opcode, index);
            size += index < 256 ? 2 : 4;
        }

        private void jump(int opcode, Label label) {
            method.visitJumpInsn(opcode, label);
            size += 3;
        }

        private void invoke(int opcode, String owner, String name, String descriptor) {
            method.visitMethodInsn(opcode, owner, name, descriptor, false);
            size += 3;
        }

        private void function(String name, String descriptor) {
            invoke(INVOKESTATIC, FUNCTIONS, name, descriptor);
        }

        private void truth() {
            function("truth", "(D)Z");
        }
    }

    /** One loader per model, so a reload's programs are collected with their models. */
    private static final class Loader extends ClassLoader {

        Loader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
