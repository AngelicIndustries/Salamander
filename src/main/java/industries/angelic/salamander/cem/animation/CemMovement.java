package industries.angelic.salamander.cem.animation;

/** Normalized movement in the body's facing plane, including remote-player position updates. */
public final class CemMovement {

    private CemMovement() {}

    public static double[] direction(double dx, double dz, double yaw) {
        double length = Math.hypot(dx, dz);
        if (length < 1e-7) return new double[] { 0, 0 };
        double radians = Math.toRadians(yaw);
        return new double[] { (-dx * Math.sin(radians) + dz * Math.cos(radians)) / length,
            (-dx * Math.cos(radians) - dz * Math.sin(radians)) / length };
    }
}
