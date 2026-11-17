package industries.angelic.salamander.cem.animation;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

/** EMF's functions give EMF's values, its quirks included. */
public class CemEmfFunctionsTest {

    private static double evaluate(String expression) {
        return CemExpressionParser.compile(expression, name -> null)
            .evaluate(new CemExpression.Context(new double[0], new HashMap<>(), new HashMap<>()));
    }

    @Test
    public void keyframesInterpolateWithCatmullRom() {
        assertEquals(15, evaluate("keyframe(1.5, 0, 10, 20, 30)"), 1e-6);
        // EMF holds the first frame until k reaches 1, and the last from the last index on.
        assertEquals(0, evaluate("keyframe(0.5, 0, 10, 20, 30)"), 0);
        assertEquals(30, evaluate("keyframe(5, 0, 10, 20, 30)"), 0);
        assertEquals(2.5, evaluate("keyframeloop(4.5, 0, 10, 20, 30)"), 1e-6);
        assertEquals(10, evaluate("keyframeloop(-3, 0, 10, 20, 30)"), 1e-6);
    }

    @Test
    public void easingsMatchEmfIncludingItsQuirks() {
        assertEquals(2.5, evaluate("easeinquad(0.5, 0, 10)"), 1e-6);
        assertEquals(5, evaluate("easeinoutsine(0.5, 0, 10)"), 1e-5);
        assertEquals(evaluate("easeinoutsine(0.3, 2, 8)"), evaluate("easeinout(0.3, 2, 8)"), 0);
        // EMF adds 1 outside the scaled delta, so only a 0..1 range ends at its end value.
        assertEquals(6, evaluate("easeoutcubic(1, 5, 10)"), 0);
        assertEquals(1, evaluate("easeoutbounce(1, 0, 1)"), 1e-6);
        // EMF's in-out back curve overshoots its own midpoint: ((u * u * (7u + 2.5) + 2) * 2 at u = -0.5.
        assertEquals(3.5, evaluate("easeinoutback(0.5, 0, 1)"), 1e-6);
    }

    @Test
    public void splinesAndAngles() {
        assertEquals(0.5, evaluate("quadbezier(0.5, 0, 0.5, 1)"), 1e-6);
        assertEquals(0.5, evaluate("cubicbezier(0.5, 0, 0, 1, 1)"), 1e-6);
        assertEquals(1, evaluate("hermite(1, 0, 1, 5, 5)"), 1e-6);
        assertEquals(20, evaluate("degdiff(10, 350)"), 0);
        assertEquals(0, evaluate("degdiff(10, 370)"), 0);
        assertEquals(Math.PI / 2, evaluate("raddiff(0, pi * 1.5)"), 1e-5);
    }

    @Test
    public void seededRandomIsOptifines() {
        int bits = Float.floatToIntBits(5f);
        bits = bits ^ 61 ^ bits >> 16;
        bits += bits << 3;
        bits ^= bits >> 4;
        bits *= 668265261;
        bits ^= bits >> 15;
        double expected = (float) Math.abs(bits) / 2.14748365E9F;
        assertEquals(expected, evaluate("random(5)"), 0);
        assertEquals(expected >= 0.5 ? 1 : 0, evaluate("randomb(5)"), 0);
    }

    @Test
    public void catchConstantsAndGlobals() {
        assertEquals(3, evaluate("catch(nan, 3)"), 0);
        assertEquals(4, evaluate("catch(4, 3)"), 0);
        assertEquals(2, evaluate("catch(0 / 0, 2, missing value)"), 0);
        assertEquals(Math.E, evaluate("e"), 0);
        assertTrue(Double.isNaN(evaluate("nan")));
        CemVariables.GLOBAL.set(CemVariables.slot("global_var.shared"), 7);
        assertEquals(
            8,
            CemExpressionParser
                .compile("global_var.shared + 1", name -> CemSlot.of(CemSlot.GLOBAL, CemVariables.slot(name)))
                .evaluate(new CemExpression.Context(new double[0], new HashMap<>(), new HashMap<>())),
            0);
        CemVariables.GLOBAL.clear();
    }
}
