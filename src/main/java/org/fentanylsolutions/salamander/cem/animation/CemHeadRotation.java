package org.fentanylsolutions.salamander.cem.animation;

import net.minecraft.util.MathHelper;

/** Network head yaw approaches its target over three ticks, as in modern Minecraft. */
public final class CemHeadRotation {

    private float target;
    private int steps;

    public void target(float yaw) {
        target = yaw;
        steps = 3;
    }

    /** Called after vanilla records the previous yaw, leaving render interpolation intact. */
    public float tick(float current) {
        if (steps == 0) return current;
        return current + MathHelper.wrapAngleTo180_float(target - current) / steps--;
    }

    /** Client living entities expose the packet target separately from the immediate head setter. */
    public interface Access {

        void salamander$lerpHeadTo(float yaw);
    }
}
