package industries.angelic.salamander.cem.animation;

import java.util.Map;
import java.util.Random;

/**
 * CEM's operators and functions beyond plain double arithmetic. The interpreter and the bytecode compiler both call
 * these, so the two give the same results.
 */
public final class CemFunctions {

    private CemFunctions() {}

    public static boolean truth(double value) {
        return value != 0 && !Double.isNaN(value);
    }

    /** A {@code varb} or {@code visible} value. */
    public static double bool(double value) {
        return truth(value) ? 1 : 0;
    }

    public static double not(double value) {
        return truth(value) ? 0 : 1;
    }

    // CEM numeric comparisons use float precision, matching native model poses.

    public static double eq(double left, double right) {
        return (float) left == (float) right ? 1 : 0;
    }

    public static double ne(double left, double right) {
        return (float) left != (float) right ? 1 : 0;
    }

    public static double ge(double left, double right) {
        return (float) left >= (float) right ? 1 : 0;
    }

    public static double le(double left, double right) {
        return (float) left <= (float) right ? 1 : 0;
    }

    public static double gt(double left, double right) {
        return (float) left > (float) right ? 1 : 0;
    }

    public static double lt(double left, double right) {
        return (float) left < (float) right ? 1 : 0;
    }

    public static double frac(double x) {
        return x - Math.floor(x);
    }

    public static double round(double x) {
        return Math.round(x);
    }

    public static double wrapRad(double x) {
        return wrap(x, Math.PI);
    }

    public static double wrapDeg(double x) {
        return wrap(x, 180);
    }

    private static double wrap(double value, double halfTurn) {
        double wrapped = value % (2 * halfTurn);
        if (wrapped >= halfTurn) wrapped -= 2 * halfTurn;
        if (wrapped < -halfTurn) wrapped += 2 * halfTurn;
        return wrapped;
    }

    public static double fmod(double x, double y) {
        return x - y * Math.floor(x / y);
    }

    public static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(max, x));
    }

    public static double between(double x, double min, double max) {
        return (float) x >= (float) min && (float) x <= (float) max ? 1 : 0;
    }

    public static double equals(double x, double y, double epsilon) {
        return Math.abs((float) x - (float) y) <= (float) epsilon ? 1 : 0;
    }

    public static double lerp(double delta, double from, double to) {
        return from + delta * (to - from);
    }

    public static double random() {
        return Math.random();
    }

    public static double random(double seed) {
        return new Random((long) seed).nextFloat();
    }

    public static double input(Map<String, Double> inputs, String name) {
        return inputs.getOrDefault(name, 0d);
    }
}
