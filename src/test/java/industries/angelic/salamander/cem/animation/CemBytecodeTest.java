package industries.angelic.salamander.cem.animation;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

/** Compiled programs give the interpreter's results bit for bit, including NaN, infinities and signed zeros. */
public class CemBytecodeTest {

    private static final String[] EXPRESSIONS = { "a + b * c - a / b % c", "-a", "!a", "!!b", "a == b", "a != b",
        "a >= b", "a <= b", "a > b", "a < b", "a || b", "a && b", "a || b && c", "if(a > 0, b, c > 0, a, c)",
        "ifb(a, b, c)", "min(a)", "min(a, b, c)", "max(a, b, c)", "max(min(a, b), c)", "in(a, b, c, 1, 2.5)",
        "in(a, b)", "sin(a) + cos(b) + tan(c)", "asin(a / 10) + acos(b / 10) + atan(c)", "atan2(a, b)",
        "torad(a) + todeg(b)", "abs(a) + floor(b) + ceil(c) + round(a)", "sqrt(abs(a)) + exp(b / 10)", "log(abs(c))",
        "frac(a) + signum(b)", "wraprad(a) + wrapdeg(b)", "pow(a, b)", "fmod(a, b)", "clamp(a, b, c)",
        "between(a, b, c)", "equals(a, b, c)", "lerp(a, b, c)", "random(a)", "pi * true - false + 1.5e2", "var.acc + a",
        "var.acc * 2", "varb.flag + b", "x.y", "print(1, 1000000, a + b)", "printb(2, 1000000, a)",
        "if(in(a, 1, 2), max(b, if(c, 1, 2)), -min(a, c))" };
    private static final double[] VALUES = { 0, -0d, 1, -1, 0.5, 2.5, -3.7, Math.PI, 1e9, Double.NaN,
        Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };

    private static Function<String, CemExpression> resolver(Map<String, Integer> inputs) {
        return name -> {
            if (name.startsWith("var.") || name.startsWith("varb."))
                return CemSlot.of(CemSlot.VARIABLE, CemVariables.slot(name));
            Integer input = inputs.get(name);
            if (input != null) return CemSlot.of(CemSlot.INPUT, input);
            // A resolved value the compiler does not know, as other callers' resolvers give.
            return name.equals("x.y") ? c -> 42 : null;
        };
    }

    private static final class Program {

        final List<CemBytecode.Assignment> assignments = new ArrayList<>();
        final List<CemExpression> interpreted = new ArrayList<>();

        void add(String target, boolean bool, CemSyntax syntax) {
            assignments.add(new CemBytecode.Assignment(CemSlot.VARIABLE, CemVariables.slot(target), bool, syntax));
            interpreted.add(syntax.interpret());
        }

        CemVariables interpret(CemExpression.Context template) {
            CemVariables variables = new CemVariables();
            CemExpression.Context context = context(variables, template);
            for (int i = 0; i < assignments.size(); i++) {
                double value = interpreted.get(i)
                    .evaluate(context);
                if (assignments.get(i).bool) value = CemFunctions.bool(value);
                variables.set(assignments.get(i).index, value);
            }
            return variables;
        }

        CemVariables run(CemProgram program, CemExpression.Context template) {
            CemVariables variables = new CemVariables();
            program.run(context(variables, template));
            return variables;
        }

        private static CemExpression.Context context(CemVariables variables, CemExpression.Context template) {
            return new CemExpression.Context(
                template.pose,
                variables,
                template.inputs,
                template.renderProperties,
                template.inputValues,
                template.renderValues);
        }
    }

    private static Program program() {
        Map<String, Integer> inputs = new HashMap<>();
        inputs.put("a", 0);
        inputs.put("b", 1);
        inputs.put("c", 2);
        Program program = new Program();
        for (int i = 0; i < EXPRESSIONS.length; i++) {
            String target = i % 7 == 0 ? "varb.flag" : i % 5 == 0 ? "var.acc" : "var.r" + i;
            program
                .add(target, target.startsWith("varb."), CemExpressionParser.parse(EXPRESSIONS[i], resolver(inputs)));
        }
        program.add("var.nbt", false, new CemSyntax.Input("nbt_query"));
        return program;
    }

    @Test
    public void compiledMatchesInterpreterOnEveryInputCombination() throws Exception {
        Program program = program();
        CemProgram compiled = CemBytecode.compile("minecraft:optifine/cem/test.jem", program.assignments);
        int combinations = 0;
        for (double a : VALUES) for (double b : VALUES) for (double c : VALUES) {
            Map<String, Double> inputs = new HashMap<>();
            inputs.put("nbt_query", a - b);
            // Not a multiple of print()'s interval, so the test does not flood the log.
            inputs.put("frame_counter", 1d);
            CemExpression.Context template = new CemExpression.Context(
                new double[0],
                new CemVariables(),
                inputs,
                new HashMap<>(),
                new double[] { a, b, c },
                new double[0]);
            CemVariables expected = program.interpret(template), actual = program.run(compiled, template);
            assertEquals(expected.keySet(), actual.keySet());
            for (String name : expected.keySet()) assertEquals(
                name + " at a=" + a + " b=" + b + " c=" + c,
                Double.doubleToLongBits(expected.get(name)),
                Double.doubleToLongBits(actual.get(name)));
            combinations++;
        }
        assertEquals(VALUES.length * VALUES.length * VALUES.length, combinations);
    }

    @Test
    public void largeProgramsSplitIntoMethodsAndKeepTheirOrder() throws Exception {
        Map<String, Integer> inputs = new HashMap<>();
        inputs.put("a", 0);
        Program program = new Program();
        // Each assignment reads the one before it, so a split that reordered them would show.
        program.add("var.chain0", false, CemExpressionParser.parse("a", resolver(inputs)));
        for (int i = 1; i < 3000; i++) program.add(
            "var.chain" + i,
            false,
            CemExpressionParser.parse("sin(var.chain" + (i - 1) + ") * 0.5 + a * " + i, resolver(inputs)));
        CemProgram compiled = CemBytecode.compile("large", program.assignments);
        long methods = java.util.Arrays.stream(
            compiled.getClass()
                .getDeclaredMethods())
            .filter(
                method -> method.getName()
                    .startsWith("run"))
            .count();
        assertTrue("expected the program split into several methods, got " + methods, methods > 3);
        CemExpression.Context template = new CemExpression.Context(
            new double[0],
            new CemVariables(),
            new HashMap<>(),
            new HashMap<>(),
            new double[] { 0.25 },
            new double[0]);
        CemVariables expected = program.interpret(template), actual = program.run(compiled, template);
        for (int i = 0; i < 3000; i++) assertEquals(
            Double.doubleToLongBits(expected.get("var.chain" + i)),
            Double.doubleToLongBits(actual.get("var.chain" + i)));
    }
}
