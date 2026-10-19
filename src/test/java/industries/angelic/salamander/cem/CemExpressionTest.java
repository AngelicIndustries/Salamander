package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;

import org.junit.Test;

import industries.angelic.salamander.cem.animation.CemExpression;
import industries.angelic.salamander.cem.animation.CemExpressionParser;

public class CemExpressionTest {

    private double evaluate(String expression) {
        return CemExpressionParser.compile(expression, name -> {
            if (name.equals("unreachable")) return c -> { throw new AssertionError("evaluated dead branch"); };
            return null;
        })
            .evaluate(new CemExpression.Context(new double[0], new HashMap<>(), new HashMap<>()));
    }

    @Test
    public void precedenceRadiansAndScientificLiterals() {
        assertEquals(7, evaluate("1 + 2 * 3"), 0);
        assertEquals(1, evaluate("sin(pi/2)"), 1e-8);
        assertEquals(90, evaluate("todeg(asin(1))"), 1e-8);
        assertEquals(1, evaluate("cos(torad(360))"), 1e-8);
        assertEquals(.012, evaluate("1.2e-2"), 1e-8);
        assertEquals(1, evaluate("!false && 3>=2 && 4!=5 || false"), 0);
        assertEquals(2, evaluate("fmod(-1,3)"), 0);
    }

    @Test
    public void angleWrappingHandlesNegativeTurnsAndTheHalfTurnBoundary() {
        assertEquals(-Math.PI, evaluate("wraprad(pi)"), 1e-8);
        assertEquals(-Math.PI, evaluate("wraprad(-3*pi)"), 1e-8);
        assertEquals(Math.PI / 2, evaluate("wraprad(-7*pi/2)"), 1e-8);
        assertEquals(-180, evaluate("wrapdeg(540)"), 0);
        assertEquals(90, evaluate("wrapdeg(-630)"), 0);
    }

    @Test
    public void nativeFloatPoseComparisonsDoNotInventGrazingOrMissExactAngles() {
        // A modern foal's baked neck pivot and a native leg angle widen from floats at the API boundary.
        assertEquals(0, evaluate("11.110297203063965 > 4+7.110297"), 0);
        assertEquals(0, evaluate("11.110297203063965 < 4+7.110297"), 0);
        assertEquals(1, evaluate("11.110297203063965 == 4+7.110297"), 0);
        assertEquals(1, evaluate("0.7853981852531433 == pi/4"), 0);
        assertEquals(0, evaluate("0.7853981852531433 != pi/4"), 0);
        assertEquals(1, evaluate("equals(0.7853981852531433,pi/4,0)"), 0);
        assertEquals(1, evaluate("in(0.7853981852531433,0,pi/4,pi)"), 0);
        assertEquals(1, evaluate("between(11.110297203063965,0,4+7.110297)"), 0);
        assertEquals(1, evaluate("11.110299 > 4+7.110297"), 0);
    }

    @Test
    public void shortCircuitsConditionsAndBooleanOperators() {
        assertEquals(4, evaluate("if(false, unreachable, true, 4, unreachable)"), 0);
        assertEquals(1, evaluate("true || unreachable"), 0);
        assertEquals(0, evaluate("false && unreachable"), 0);
        assertEquals(1, evaluate("between(3,2,4) && equals(2,2.01,0.1)"), 0);
    }

    @Test
    public void seededRandomIsStableAndInvalidExpressionsAreRejected() {
        assertEquals(evaluate("random(42)"), evaluate("random(42)"), 0);
        assertNotEquals(evaluate("random(42)"), evaluate("random(43)"), 0);
        for (String expression : new String[] { "sin()", "if(1,2)", "1 trailing", "unknown", "sin(1", "unknown(1)" })
            assertThrows(expression, IllegalArgumentException.class, () -> evaluate(expression));
        // A flat sum still creates a nested evaluation tree; reject it before it can exhaust the render stack.
        assertThrows(IllegalArgumentException.class, () -> evaluate(String.join("+", Collections.nCopies(2000, "1"))));
    }
}
