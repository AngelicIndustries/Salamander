package industries.angelic.salamander.cem.animation;

/**
 * A parsed CEM expression: {@link #interpret()} builds the closure interpreter, {@link CemBytecode} compiles it.
 */
public abstract class CemSyntax {

    private CemSyntax() {}

    public abstract CemExpression interpret();

    public static final class Constant extends CemSyntax {

        public final double value;

        Constant(double value) {
            this.value = value;
        }

        @Override
        public CemExpression interpret() {
            double number = value;
            return c -> number;
        }
    }

    /** A name the parser's resolver gave a value for: a model slot ({@link CemSlot}) or any other expression. */
    public static final class Reference extends CemSyntax {

        public final CemExpression value;

        Reference(CemExpression value) {
            this.value = value;
        }

        @Override
        public CemExpression interpret() {
            return value;
        }
    }

    /** An input read by name each time, such as an {@code nbt()} query. */
    public static final class Input extends CemSyntax {

        public final String name;

        Input(String name) {
            this.name = name;
        }

        @Override
        public CemExpression interpret() {
            String input = name;
            return c -> c.inputs.getOrDefault(input, 0d);
        }
    }

    /** {@code !} or {@code -}. */
    public static final class Unary extends CemSyntax {

        public final char operator;
        public final CemSyntax operand;

        Unary(char operator, CemSyntax operand) {
            this.operator = operator;
            this.operand = operand;
        }

        @Override
        public CemExpression interpret() {
            CemExpression value = operand.interpret();
            if (operator == '!') return c -> CemFunctions.not(value.evaluate(c));
            return c -> -value.evaluate(c);
        }
    }

    /** Short-circuit boolean operators, float-precision comparisons and double arithmetic. */
    public static final class Binary extends CemSyntax {

        public final String operator;
        public final CemSyntax left;
        public final CemSyntax right;

        Binary(String operator, CemSyntax left, CemSyntax right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        public CemExpression interpret() {
            CemExpression l = left.interpret(), r = right.interpret();
            switch (operator) {
                case "||":
                    return c -> CemFunctions.truth(l.evaluate(c)) || CemFunctions.truth(r.evaluate(c)) ? 1 : 0;
                case "&&":
                    return c -> CemFunctions.truth(l.evaluate(c)) && CemFunctions.truth(r.evaluate(c)) ? 1 : 0;
                case "==":
                    return c -> CemFunctions.eq(l.evaluate(c), r.evaluate(c));
                case "!=":
                    return c -> CemFunctions.ne(l.evaluate(c), r.evaluate(c));
                case ">=":
                    return c -> CemFunctions.ge(l.evaluate(c), r.evaluate(c));
                case "<=":
                    return c -> CemFunctions.le(l.evaluate(c), r.evaluate(c));
                case ">":
                    return c -> CemFunctions.gt(l.evaluate(c), r.evaluate(c));
                case "<":
                    return c -> CemFunctions.lt(l.evaluate(c), r.evaluate(c));
                case "+":
                    return c -> l.evaluate(c) + r.evaluate(c);
                case "-":
                    return c -> l.evaluate(c) - r.evaluate(c);
                case "*":
                    return c -> l.evaluate(c) * r.evaluate(c);
                case "/":
                    return c -> l.evaluate(c) / r.evaluate(c);
                case "%":
                    return c -> l.evaluate(c) % r.evaluate(c);
                default:
                    throw new IllegalStateException(operator);
            }
        }
    }

    /** A function call, its arity already checked by the parser. */
    public static final class Call extends CemSyntax {

        public final String name;
        public final CemSyntax[] arguments;

        Call(String name, CemSyntax[] arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public CemExpression interpret() {
            CemExpression[] args = new CemExpression[arguments.length];
            for (int i = 0; i < args.length; i++) args[i] = arguments[i].interpret();
            int count = args.length;
            switch (name) {
                case "if":
                case "ifb":
                    return c -> {
                        for (int i = 0; i < count - 1; i += 2) {
                            if (CemFunctions.truth(args[i].evaluate(c))) return args[i + 1].evaluate(c);
                        }
                        return args[count - 1].evaluate(c);
                    };
                case "min":
                case "max": {
                    boolean min = name.equals("min");
                    return c -> {
                        double value = args[0].evaluate(c);
                        for (int i = 1; i < count; i++)
                            value = min ? Math.min(value, args[i].evaluate(c)) : Math.max(value, args[i].evaluate(c));
                        return value;
                    };
                }
                case "random":
                    return count == 0 ? c -> CemFunctions.random() : c -> CemFunctions.random(args[0].evaluate(c));
                case "in":
                    return c -> {
                        float value = (float) args[0].evaluate(c);
                        for (int i = 1; i < count; i++) if (value == (float) args[i].evaluate(c)) return 1;
                        return 0;
                    };
                case "print":
                case "printb": {
                    boolean bool = name.equals("printb");
                    return c -> {
                        double value = args[2].evaluate(c);
                        long interval = Math.max(1, (long) args[1].evaluate(c));
                        long frame = c.inputs.getOrDefault("frame_counter", 0d)
                            .longValue();
                        if (frame % interval == 0) java.util.logging.Logger.getLogger("Salamander CEM")
                            .info(
                                "[" + (int) args[0].evaluate(c)
                                    + "] "
                                    + (bool ? Boolean.toString(CemFunctions.truth(value)) : Double.toString(value)));
                        return bool ? CemFunctions.bool(value) : value;
                    };
                }
                default:
                    return fixed(args);
            }
        }

        private CemExpression fixed(CemExpression[] args) {
            int count = args.length;
            String function = name;
            return c -> {
                double x = args[0].evaluate(c);
                double y = count > 1 ? args[1].evaluate(c) : 0;
                double z = count > 2 ? args[2].evaluate(c) : 0;
                switch (function) {
                    case "sin":
                        return Math.sin(x);
                    case "cos":
                        return Math.cos(x);
                    case "tan":
                        return Math.tan(x);
                    case "asin":
                        return Math.asin(x);
                    case "acos":
                        return Math.acos(x);
                    case "atan":
                        return Math.atan(x);
                    case "atan2":
                        return Math.atan2(x, y);
                    case "torad":
                        return Math.toRadians(x);
                    case "todeg":
                        return Math.toDegrees(x);
                    case "abs":
                        return Math.abs(x);
                    case "floor":
                        return Math.floor(x);
                    case "ceil":
                        return Math.ceil(x);
                    case "round":
                        return CemFunctions.round(x);
                    case "sqrt":
                        return Math.sqrt(x);
                    case "exp":
                        return Math.exp(x);
                    case "log":
                        return Math.log(x);
                    case "frac":
                        return CemFunctions.frac(x);
                    case "wraprad":
                        return CemFunctions.wrapRad(x);
                    case "wrapdeg":
                        return CemFunctions.wrapDeg(x);
                    case "signum":
                        return Math.signum(x);
                    case "pow":
                        return Math.pow(x, y);
                    case "fmod":
                        return CemFunctions.fmod(x, y);
                    case "clamp":
                        return CemFunctions.clamp(x, y, z);
                    case "between":
                        return CemFunctions.between(x, y, z);
                    case "equals":
                        return CemFunctions.equals(x, y, z);
                    case "lerp":
                        return CemFunctions.lerp(x, y, z);
                    default:
                        throw new IllegalStateException(function);
                }
            };
        }
    }
}
