package industries.angelic.salamander.cem.animation;

import java.util.Map;

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

    /** OptiFine's seeded random, as EMF reproduces it: the seed's float bits through OptiFine's integer hash. */
    public static double random(double seed) {
        return (float) Math.abs(hash(Float.floatToIntBits((float) seed))) / 2.14748365E9F;
    }

    public static double randomb() {
        return random() >= 0.5f ? 1 : 0;
    }

    public static double randomb(double seed) {
        return random(seed) >= 0.5f ? 1 : 0;
    }

    /** OptiFine's integer hash. */
    public static int hash(int x) {
        x = x ^ 61 ^ x >> 16;
        x += x << 3;
        x ^= x >> 4;
        x *= 668265261;
        x ^= x >> 15;
        return x;
    }

    public static boolean isNaN(double value) {
        return Double.isNaN(value);
    }

    // EMF's functions below compute in float, as EMF does, so packs tuned for EMF get its exact curves, quirks
    // included: some easings add 1 outside the scaled delta, and keyframe() holds its first frame below k = 1.

    /** Mth.catmullrom. */
    public static double catmullrom(double delta, double p0, double p1, double p2, double p3) {
        return catmullrom((float) delta, (float) p0, (float) p1, (float) p2, (float) p3);
    }

    private static float catmullrom(float d, float p0, float p1, float p2, float p3) {
        return 0.5f * (2 * p1 + (p2 - p0) * d
            + (2 * p0 - 5 * p1 + 4 * p2 - p3) * d * d
            + (3 * p1 - p0 - 3 * p2 + p3) * d * d * d);
    }

    public static double keyframe(double k, double[] frames) {
        float delta = (float) k;
        int floor = floor(delta), end = frames.length - 1;
        if (floor >= end) return (float) frames[end];
        if (floor <= 0) return (float) frames[0];
        return catmullrom(
            delta - floor,
            (float) frames[clamp(floor - 1, end)],
            (float) frames[clamp(floor, end)],
            (float) frames[clamp(floor + 1, end)],
            (float) frames[clamp(floor + 2, end)]);
    }

    public static double keyframeloop(double k, double[] frames) {
        float delta = (float) k;
        int floor = floor(delta), n = frames.length;
        return catmullrom(
            delta - floor,
            (float) frames[((floor - 1) % n + n) % n],
            (float) frames[(floor % n + n) % n],
            (float) frames[((floor + 1) % n + n) % n],
            (float) frames[((floor + 2) % n + n) % n]);
    }

    private static int floor(float value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    private static int clamp(int value, int end) {
        return value < 0 ? 0 : Math.min(value, end);
    }

    public static double quadbezier(double t, double p0, double p1, double p2) {
        float u = 1f - (float) t, f = (float) t;
        return u * u * (float) p0 + 2f * u * f * (float) p1 + f * f * (float) p2;
    }

    public static double cubicbezier(double t, double p0, double p1, double p2, double p3) {
        float f = (float) t, u = 1f - f, u2 = u * u, f2 = f * f;
        return u2 * u * (float) p0 + 3f * u2 * f * (float) p1 + 3f * u * f2 * (float) p2 + f2 * f * (float) p3;
    }

    public static double hermite(double t, double p0, double p1, double m0, double m1) {
        float f = (float) t, f2 = f * f, f3 = f2 * f;
        float h00 = 2 * f3 - 3 * f2 + 1, h10 = f3 - 2 * f2 + f, h01 = -2 * f3 + 3 * f2, h11 = f3 - f2;
        return h00 * (float) p0 + h10 * (float) m0 + h01 * (float) p1 + h11 * (float) m1;
    }

    /** Mth.degreesDifferenceAbs. */
    public static double degdiff(double from, double to) {
        return Math.abs(wrapDegrees((float) to - (float) from));
    }

    public static double raddiff(double from, double to) {
        float degrees = Math.abs(wrapDegrees((float) to * RAD_TO_DEG - (float) from * RAD_TO_DEG));
        return degrees * DEG_TO_RAD;
    }

    /** Mth's constants: RAD_TO_DEG is 180f / (float) PI, one ulp off 180 / PI rounded. */
    private static final float DEG_TO_RAD = 0.017453292f, RAD_TO_DEG = 57.295776f;

    private static float wrapDegrees(float value) {
        float wrapped = value % 360f;
        if (wrapped >= 180f) wrapped -= 360f;
        if (wrapped < -180f) wrapped += 360f;
        return wrapped;
    }

    /** One of EMF's easings by its CEM name, or null. */
    public static Easing easing(String name) {
        return EASINGS.get(name);
    }

    @FunctionalInterface
    public interface Easing {

        float ease(float t, float start, float end);
    }

    private static final java.util.Map<String, Easing> EASINGS = new java.util.HashMap<>();
    static {
        EASINGS.put("easeinquad", (t, s, e) -> s + (e - s) * t * t);
        EASINGS.put("easeoutquad", (t, s, e) -> s + (e - s) * -t * (t - 2));
        EASINGS.put(
            "easeinoutquad",
            (t, s, e) -> t < 0.5 ? s + (e - s) * (2 * t * t) : s + (e - s) * (-2 * t * (t - 2) - 1));
        EASINGS.put("easeincubic", (t, s, e) -> s + (e - s) * t * t * t);
        EASINGS.put("easeoutcubic", (t, s, e) -> {
            float u = t - 1;
            return s + (e - s) * u * u * u + 1;
        });
        EASINGS.put("easeinoutcubic", (t, s, e) -> {
            if (t < 0.5) return s + (e - s) * 4 * t * t * t;
            float u = t - 1;
            return s + (e - s) * u * (2 * u * u + 2) + 1;
        });
        EASINGS.put("easeinquart", (t, s, e) -> s + (e - s) * t * t * t * t);
        EASINGS.put("easeoutquart", (t, s, e) -> {
            float u = t - 1;
            return s + (e - s) * u * u * u * u + 1;
        });
        EASINGS.put("easeinoutquart", (t, s, e) -> {
            if (t < 0.5) return s + (e - s) * 8 * t * t * t * t;
            float u = t - 1;
            return s + (e - s) * u * (8 * u * u * u + 1) + 1;
        });
        EASINGS.put("easeinquint", (t, s, e) -> s + (e - s) * t * t * t * t * t);
        EASINGS.put("easeoutquint", (t, s, e) -> {
            float u = t - 1;
            return s + (e - s) * u * u * u * u * u + 1;
        });
        EASINGS.put("easeinoutquint", (t, s, e) -> {
            if (t < 0.5) return s + (e - s) * 16 * t * t * t * t * t;
            float u = t - 1;
            return s + (e - s) * u * (16 * u * u * u * u + 1) + 1;
        });
        EASINGS.put("easeinsine", (t, s, e) -> s + (e - s) * (1 - (float) Math.cos(t * Math.PI / 2)));
        EASINGS.put("easeoutsine", (t, s, e) -> s + (e - s) * (float) Math.sin(t * Math.PI / 2));
        EASINGS.put("easeinoutsine", (t, s, e) -> s + (e - s) * (float) (-0.5 * (Math.cos(Math.PI * t) - 1)));
        EASINGS.put("easeinexpo", (t, s, e) -> s + (e - s) * (float) Math.pow(2, 10 * (t - 1)));
        EASINGS.put("easeoutexpo", (t, s, e) -> s + (e - s) * (float) (-Math.pow(2, -10 * t) + 1));
        EASINGS.put(
            "easeinoutexpo",
            (t, s, e) -> t < 1 ? s + (e - s) * (float) (0.5 * Math.pow(2, 10 * (t - 1)))
                : s + (e - s) * (float) (0.5 * (-Math.pow(2, -10 * (t - 1)) + 2)));
        EASINGS.put("easeincirc", (t, s, e) -> s + (e - s) * (float) -(Math.sqrt(1 - t * t) - 1));
        EASINGS.put("easeoutcirc", (t, s, e) -> {
            float u = t - 1;
            return s + (e - s) * (float) Math.sqrt(1 - u * u);
        });
        EASINGS.put("easeinoutcirc", (t, s, e) -> {
            float t2 = t * 2;
            if (t2 < 1) return s + (e - s) * (float) (-0.5 * (Math.sqrt(1 - t2 * t2) - 1));
            float u = t2 - 2;
            return s + (e - s) * (float) (0.5 * (Math.sqrt(1 - u * u) + 1));
        });
        EASINGS.put("easeinelastic", (t, s, e) -> {
            float u = t - 1;
            return s + (e - s) * (float) (-Math.pow(2, 10 * u) * Math.sin((u - 0.3 / 4) * (2 * Math.PI) / 0.3));
        });
        EASINGS.put(
            "easeoutelastic",
            (t, s, e) -> s
                + (e - s) * (float) (Math.pow(2, -10 * t) * Math.sin((t - 0.3 / 4) * (2 * Math.PI) / 0.3) + 1));
        EASINGS.put("easeinoutelastic", (t, s, e) -> {
            float u = t - 1;
            if (t < 0.5) return s
                + (e - s) * (float) (-0.5 * Math.pow(2, 10 * u) * Math.sin((u - 0.225 / 4) * (2 * Math.PI) / 0.45));
            return s
                + (e - s) * (float) (0.5 * Math.pow(2, -10 * u) * Math.sin((u - 0.225 / 4) * (2 * Math.PI) / 0.45) + 1);
        });
        EASINGS.put("easeinbounce", (t, s, e) -> s + (e - s) * (1 - bounceOut(1 - t)));
        EASINGS.put("easeoutbounce", CemFunctions::easeOutBounce);
        EASINGS.put(
            "easeinoutbounce",
            (t, s, e) -> t < 0.5 ? s + (e - s) * (0.5f * (1 - bounceOut(1 - t * 2)))
                : s + (e - s) * (0.5f * bounceOut(t * 2 - 1) + 0.5f));
        EASINGS.put("easeinback", (t, s, e) -> s + (e - s) * (t * t * (2.70158f * t - 1.70158f)));
        EASINGS.put("easeoutback", (t, s, e) -> {
            float u = t - 1;
            return s + (e - s) * (u * u * (2.70158f * u + 1.70158f) + 1);
        });
        EASINGS.put("easeinoutback", (t, s, e) -> {
            if (t < 0.5) return s + (e - s) * (t * t * (7 * t - 2.5f) * 2);
            float u = t - 1;
            return s + (e - s) * ((u * u * (7 * u + 2.5f) + 2) * 2);
        });
        // EMF's older names.
        EASINGS.put("easein", EASINGS.get("easeinsine"));
        EASINGS.put("easeout", EASINGS.get("easeoutsine"));
        EASINGS.put("easeinout", EASINGS.get("easeinoutsine"));
        EASINGS.put("cubiceasein", EASINGS.get("easeincubic"));
        EASINGS.put("cubiceaseout", EASINGS.get("easeoutcubic"));
        EASINGS.put("cubiceaseinout", EASINGS.get("easeinoutcubic"));
    }

    /** EMF's easeOutBounce: past the first segment it adds start and delta in double before rounding. */
    private static float easeOutBounce(float t, float start, float end) {
        float delta = end - start;
        if (t < 1 / 2.75) return start + delta * (7.5625f * t * t);
        if (t < 2 / 2.75) {
            t -= (float) (1.5 / 2.75);
            return (float) (start + delta * (7.5625f * t * t + .75));
        }
        if (t < 2.5 / 2.75) {
            t -= (float) (2.25 / 2.75);
            return (float) (start + delta * (7.5625f * t * t + .9375));
        }
        t -= (float) (2.625 / 2.75);
        return (float) (start + delta * (7.5625f * t * t + .984375));
    }

    /** EMF's easeOutBounce(t, 0, 1). */
    private static float bounceOut(float t) {
        if (t < 1 / 2.75) return 7.5625f * t * t;
        if (t < 2 / 2.75) {
            t -= (float) (1.5 / 2.75);
            return (float) (7.5625f * t * t + .75);
        }
        if (t < 2.5 / 2.75) {
            t -= (float) (2.25 / 2.75);
            return (float) (7.5625f * t * t + .9375);
        }
        t -= (float) (2.625 / 2.75);
        return (float) (7.5625f * t * t + .984375);
    }

    public static double ease(String name, double t, double start, double end) {
        return EASINGS.get(name)
            .ease((float) t, (float) start, (float) end);
    }

    public static double input(Map<String, Double> inputs, String name) {
        return inputs.getOrDefault(name, 0d);
    }
}
