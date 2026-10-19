package industries.angelic.salamander.cem.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/** Recursive-descent compiler with short-circuit boolean operators and lazy CEM conditionals. */
public final class CemExpressionParser {

    private final String source;
    private final Function<String, CemExpression> resolver;
    private int position;
    private int depth;
    private int values;

    private CemExpressionParser(String source, Function<String, CemExpression> resolver) {
        this.source = source;
        this.resolver = resolver;
    }

    public static CemExpression compile(String source, Function<String, CemExpression> resolver) {
        if (source == null || source.length() > 65536)
            throw new IllegalArgumentException("Invalid CEM expression size");
        CemExpressionParser parser = new CemExpressionParser(source, resolver);
        CemExpression expression = parser.binary(0);
        parser.space();
        if (parser.position != source.length()) throw parser.error("Unexpected token");
        return expression;
    }

    private static final String[][] OPERATORS = { { "||" }, { "&&" }, { "==", "!=" }, { ">=", "<=", ">", "<" },
        { "+", "-" }, { "*", "/", "%" } };

    private CemExpression binary(int precedence) {
        if (precedence == OPERATORS.length) return unary();
        CemExpression value = binary(precedence + 1);
        while (true) {
            String found = null;
            for (String operator : OPERATORS[precedence]) {
                if (take(operator)) {
                    found = operator;
                    break;
                }
            }
            if (found == null) return value;
            CemExpression left = value;
            CemExpression right = binary(precedence + 1);
            switch (found) {
                case "||":
                    value = c -> truth(left.evaluate(c)) || truth(right.evaluate(c)) ? 1 : 0;
                    break;
                case "&&":
                    value = c -> truth(left.evaluate(c)) && truth(right.evaluate(c)) ? 1 : 0;
                    break;
                // CEM numeric comparisons use float precision, matching native model poses.
                case "==":
                    value = c -> (float) left.evaluate(c) == (float) right.evaluate(c) ? 1 : 0;
                    break;
                case "!=":
                    value = c -> (float) left.evaluate(c) != (float) right.evaluate(c) ? 1 : 0;
                    break;
                case ">=":
                    value = c -> (float) left.evaluate(c) >= (float) right.evaluate(c) ? 1 : 0;
                    break;
                case "<=":
                    value = c -> (float) left.evaluate(c) <= (float) right.evaluate(c) ? 1 : 0;
                    break;
                case ">":
                    value = c -> (float) left.evaluate(c) > (float) right.evaluate(c) ? 1 : 0;
                    break;
                case "<":
                    value = c -> (float) left.evaluate(c) < (float) right.evaluate(c) ? 1 : 0;
                    break;
                case "+":
                    value = c -> left.evaluate(c) + right.evaluate(c);
                    break;
                case "-":
                    value = c -> left.evaluate(c) - right.evaluate(c);
                    break;
                case "*":
                    value = c -> left.evaluate(c) * right.evaluate(c);
                    break;
                case "/":
                    value = c -> left.evaluate(c) / right.evaluate(c);
                    break;
                case "%":
                    value = c -> left.evaluate(c) % right.evaluate(c);
                    break;
                default:
                    throw error("Unknown operator");
            }
        }
    }

    private CemExpression unary() {
        if (++values > 512) throw error("Expression exceeds 512 values/operators");
        if (++depth > 128) throw error("Expression nesting exceeds 128");
        try {
            if (take("!")) {
                CemExpression value = unary();
                return c -> truth(value.evaluate(c)) ? 0 : 1;
            }
            if (take("-")) {
                CemExpression value = unary();
                return c -> -value.evaluate(c);
            }
            if (take("+")) return unary();
            if (take("(")) {
                CemExpression value = binary(0);
                expect(")");
                return value;
            }
            space();
            int start = position;
            if (position < source.length()
                && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) {
                while (position < source.length()
                    && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) position++;
                if (position < source.length() && (source.charAt(position) == 'e' || source.charAt(position) == 'E')) {
                    position++;
                    if (position < source.length()
                        && (source.charAt(position) == '+' || source.charAt(position) == '-')) position++;
                    while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
                }
                double number;
                try {
                    number = Double.parseDouble(source.substring(start, position));
                } catch (NumberFormatException exception) {
                    throw error("Invalid number");
                }
                if (!Double.isFinite(number)) throw error("Non-finite number");
                return c -> number;
            }
            while (position < source.length()) {
                char ch = source.charAt(position);
                if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '.' && ch != ':') break;
                position++;
            }
            if (start == position) throw error("Expected a value");
            String name = source.substring(start, position);
            if (take("(")) {
                List<CemExpression> arguments = new ArrayList<>();
                if (!take(")")) {
                    do {
                        arguments.add(binary(0));
                    } while (take(","));
                    expect(")");
                }
                return function(name, arguments.toArray(new CemExpression[0]));
            }
            if (name.equals("pi")) return c -> Math.PI;
            if (name.equals("true")) return c -> 1;
            if (name.equals("false")) return c -> 0;
            CemExpression value = resolver.apply(name);
            if (value == null) throw error("Unknown variable '" + name + "'");
            return value;
        } finally {
            depth--;
        }
    }

    private CemExpression function(String name, CemExpression[] args) {
        int count = args.length;
        if (name.equals("if") || name.equals("ifb")) {
            if (count < 3 || count % 2 != 1)
                throw error("'" + name + "' requires condition/value pairs and an else value");
            return c -> {
                for (int i = 0; i < count - 1; i += 2) {
                    if (truth(args[i].evaluate(c))) return args[i + 1].evaluate(c);
                }
                return args[count - 1].evaluate(c);
            };
        }
        if (name.equals("min") || name.equals("max")) {
            if (count < 1) throw error("'" + name + "' requires arguments");
            return c -> {
                double value = args[0].evaluate(c);
                for (int i = 1; i < count; i++) value = name.equals("min") ? Math.min(value, args[i].evaluate(c))
                    : Math.max(value, args[i].evaluate(c));
                return value;
            };
        }
        if (name.equals("random")) {
            if (count > 1) throw error("random accepts zero or one argument");
            return count == 0 ? c -> Math.random() : c -> new Random((long) args[0].evaluate(c)).nextFloat();
        }
        if (name.equals("in")) {
            if (count < 2) throw error("in requires at least two arguments");
            return c -> {
                float value = (float) args[0].evaluate(c);
                for (int i = 1; i < count; i++) if (value == (float) args[i].evaluate(c)) return 1;
                return 0;
            };
        }
        if (name.equals("print") || name.equals("printb")) {
            if (count != 3) throw error(name + " requires id, frame interval, and value");
            return c -> {
                double value = args[2].evaluate(c);
                long interval = Math.max(1, (long) args[1].evaluate(c));
                long frame = c.inputs.getOrDefault("frame_counter", 0d)
                    .longValue();
                if (frame % interval == 0) java.util.logging.Logger.getLogger("Salamander CEM")
                    .info(
                        "[" + (int) args[0].evaluate(c)
                            + "] "
                            + (name.equals("printb") ? Boolean.toString(truth(value)) : Double.toString(value)));
                return name.equals("printb") ? (truth(value) ? 1 : 0) : value;
            };
        }
        int required;
        switch (name) {
            case "clamp":
            case "between":
            case "equals":
            case "lerp":
                required = 3;
                break;
            case "atan2":
            case "pow":
            case "fmod":
                required = 2;
                break;
            case "sin":
            case "cos":
            case "tan":
            case "asin":
            case "acos":
            case "atan":
            case "torad":
            case "todeg":
            case "abs":
            case "floor":
            case "ceil":
            case "round":
            case "sqrt":
            case "exp":
            case "log":
            case "frac":
            case "signum":
            case "wraprad":
            case "wrapdeg":
                required = 1;
                break;
            default:
                throw error("Unsupported function '" + name + "'");
        }
        if (count != required) throw error("'" + name + "' requires " + required + " arguments");
        return c -> {
            double x = args[0].evaluate(c);
            double y = count > 1 ? args[1].evaluate(c) : 0;
            double z = count > 2 ? args[2].evaluate(c) : 0;
            switch (name) {
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
                    return Math.round(x);
                case "sqrt":
                    return Math.sqrt(x);
                case "exp":
                    return Math.exp(x);
                case "log":
                    return Math.log(x);
                case "frac":
                    return x - Math.floor(x);
                case "wraprad":
                    return wrap(x, Math.PI);
                case "wrapdeg":
                    return wrap(x, 180);
                case "signum":
                    return Math.signum(x);
                case "pow":
                    return Math.pow(x, y);
                case "fmod":
                    return x - y * Math.floor(x / y);
                case "clamp":
                    return Math.max(y, Math.min(z, x));
                case "between":
                    return (float) x >= (float) y && (float) x <= (float) z ? 1 : 0;
                case "equals":
                    return Math.abs((float) x - (float) y) <= (float) z ? 1 : 0;
                case "lerp":
                    return y + x * (z - y);
                default:
                    throw new IllegalStateException(name);
            }
        };
    }

    private static double wrap(double value, double halfTurn) {
        double wrapped = value % (2 * halfTurn);
        if (wrapped >= halfTurn) wrapped -= 2 * halfTurn;
        if (wrapped < -halfTurn) wrapped += 2 * halfTurn;
        return wrapped;
    }

    public static boolean truth(double value) {
        return value != 0 && !Double.isNaN(value);
    }

    private boolean take(String token) {
        space();
        if (!source.startsWith(token, position)) return false;
        position += token.length();
        return true;
    }

    private void expect(String token) {
        if (!take(token)) throw error("Expected '" + token + "'");
    }

    private void space() {
        while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at character " + position + " in '" + source + "'");
    }
}
