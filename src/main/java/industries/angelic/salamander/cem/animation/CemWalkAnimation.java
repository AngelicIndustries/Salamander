package industries.angelic.salamander.cem.animation;

/** Tick-based walk state for legacy entities which never update their native limb fields. */
public final class CemWalkAnimation {

    private double previousSpeed, speed, position;

    public void tick(double dx, double dz) {
        previousSpeed = speed;
        double target = Math.min(1, Math.sqrt(dx * dx + dz * dz) * 4);
        speed += (target - speed) * .4;
        position += speed;
    }

    public double speed(float partialTicks) {
        return previousSpeed + (speed - previousSpeed) * partialTicks;
    }

    public double position(float partialTicks) {
        return position - speed * (1 - partialTicks);
    }
}
