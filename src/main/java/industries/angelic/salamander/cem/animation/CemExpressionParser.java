package industries.angelic.salamander.cem.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Recursive-descent parser with short-circuit boolean operators and lazy CEM conditionals. */
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

    /** The interpreted expression. */
    public static CemExpression compile(String source, Function<String, CemExpression> resolver) {
        return parse(source, resolver).interpret();
    }

    /** The syntax tree, for the interpreter or {@link CemBytecode}. */
    public static CemSyntax parse(String source, Function<String, CemExpression> resolver) {
        if (source == null || source.length() > 65536)
            throw new IllegalArgumentException("Invalid CEM expression size");
        CemExpressionParser parser = new CemExpressionParser(source, resolver);
        CemSyntax expression = parser.binary(0);
        parser.space();
        if (parser.position != source.length()) throw parser.error("Unexpected token");
        return expression;
    }

    private static final String[][] OPERATORS = { { "||" }, { "&&" }, { "==", "!=" }, { ">=", "<=", ">", "<" },
        { "+", "-" }, { "*", "/", "%" } };

    private CemSyntax binary(int precedence) {
        if (precedence == OPERATORS.length) return unary();
        CemSyntax value = binary(precedence + 1);
        while (true) {
            String found = null;
            for (String operator : OPERATORS[precedence]) {
                if (take(operator)) {
                    found = operator;
                    break;
                }
            }
            if (found == null) return value;
            value = new CemSyntax.Binary(found, value, binary(precedence + 1));
        }
    }

    private CemSyntax unary() {
        if (++values > 512) throw error("Expression exceeds 512 values/operators");
        if (++depth > 128) throw error("Expression nesting exceeds 128");
        try {
            if (take("!")) return new CemSyntax.Unary('!', unary());
            if (take("-")) return new CemSyntax.Unary('-', unary());
            if (take("+")) return unary();
            if (take("(")) {
                CemSyntax value = binary(0);
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
                return new CemSyntax.Constant(number);
            }
            while (position < source.length()) {
                char ch = source.charAt(position);
                if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '.' && ch != ':') break;
                position++;
            }
            if (start == position) throw error("Expected a value");
            String name = source.substring(start, position);
            if (name.equals("nbt") && take("(")) {
                String[] arguments = rawArguments();
                if (arguments.length != 2) throw error("nbt requires an NBT path and a test");
                String input;
                try {
                    input = CemNbtQueries.register(arguments[0], arguments[1]);
                } catch (IllegalArgumentException exception) {
                    throw error(exception.getMessage());
                }
                return new CemSyntax.Input(input);
            }
            if (take("(")) {
                List<CemSyntax> arguments = new ArrayList<>();
                if (!take(")")) {
                    do {
                        arguments.add(binary(0));
                    } while (take(","));
                    expect(")");
                }
                return function(name, arguments.toArray(new CemSyntax[0]));
            }
            if (name.equals("pi")) return new CemSyntax.Constant(Math.PI);
            if (name.equals("true")) return new CemSyntax.Constant(1);
            if (name.equals("false")) return new CemSyntax.Constant(0);
            CemExpression value = resolver.apply(name);
            if (value == null) throw error("Unknown variable '" + name + "'");
            return new CemSyntax.Reference(value);
        } finally {
            depth--;
        }
    }

    /** Checks the arity; {@link CemSyntax.Call} interprets it and {@link CemBytecode} compiles it. */
    private CemSyntax function(String name, CemSyntax[] args) {
        int count = args.length;
        switch (name) {
            case "if":
            case "ifb":
                if (count < 3 || count % 2 != 1)
                    throw error("'" + name + "' requires condition/value pairs and an else value");
                return new CemSyntax.Call(name, args);
            case "min":
            case "max":
                if (count < 1) throw error("'" + name + "' requires arguments");
                return new CemSyntax.Call(name, args);
            case "random":
                if (count > 1) throw error("random accepts zero or one argument");
                return new CemSyntax.Call(name, args);
            case "in":
                if (count < 2) throw error("in requires at least two arguments");
                return new CemSyntax.Call(name, args);
            case "print":
            case "printb":
                if (count != 3) throw error(name + " requires id, frame interval, and value");
                return new CemSyntax.Call(name, args);
            default:
                int required = arity(name);
                if (required < 0) throw error("Unsupported function '" + name + "'");
                if (count != required) throw error("'" + name + "' requires " + required + " arguments");
                return new CemSyntax.Call(name, args);
        }
    }

    /** Arguments of the fixed-arity functions, or -1 for an unknown name. */
    static int arity(String name) {
        switch (name) {
            case "clamp":
            case "between":
            case "equals":
            case "lerp":
                return 3;
            case "atan2":
            case "pow":
            case "fmod":
                return 2;
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
                return 1;
            default:
                return -1;
        }
    }

    /** Arguments taken as text up to the closing parenthesis, split at top-level commas, as EMF reads nbt(). */
    private String[] rawArguments() {
        List<String> arguments = new ArrayList<>();
        int depth = 0, argument = position;
        while (position < source.length()) {
            char ch = source.charAt(position);
            if (ch == '(' || ch == '[' || ch == '{') depth++;
            else if (ch == ')' || ch == ']' || ch == '}') {
                if (depth == 0 && ch == ')') {
                    arguments.add(
                        source.substring(argument, position)
                            .trim());
                    position++;
                    return arguments.toArray(new String[0]);
                }
                depth--;
            } else if (ch == ',' && depth == 0) {
                arguments.add(
                    source.substring(argument, position)
                        .trim());
                argument = position + 1;
            }
            position++;
        }
        throw error("Expected ')'");
    }

    public static boolean truth(double value) {
        return CemFunctions.truth(value);
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
