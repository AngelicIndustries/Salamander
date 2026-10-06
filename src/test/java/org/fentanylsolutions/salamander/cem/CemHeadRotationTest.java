package org.fentanylsolutions.salamander.cem;

import static org.junit.Assert.*;

import net.minecraft.util.MathHelper;

import org.fentanylsolutions.salamander.cem.animation.CemHeadRotation;
import org.junit.Test;

public class CemHeadRotationTest {

    @Test
    public void packetTargetPreservesPreviousYawForSmoothRenderFrames() {
        CemHeadRotation rotation = new CemHeadRotation();
        float current = 0;
        rotation.target(45);
        float previousFrame = current;
        for (int tick = 1; tick <= 3; tick++) {
            // Vanilla saves the previous angle before the client advances toward the packet target.
            float previous = current;
            current = rotation.tick(current);
            assertEquals(tick * 15, current, 1e-5);
            for (int frame = 0; frame <= 4; frame++) {
                float rendered = previous + MathHelper.wrapAngleTo180_float(current - previous) * frame / 4;
                assertEquals((tick - 1) * 15 + frame * 3.75, rendered, 1e-5);
                assertTrue(rendered >= previousFrame);
                previousFrame = rendered;
            }
        }
        assertEquals(45, rotation.tick(current), 0);
    }

    @Test
    public void retargetingStartsFromCurrentYawAndLatestPacketWins() {
        CemHeadRotation rotation = new CemHeadRotation();
        rotation.target(90);
        float current = rotation.tick(0);
        assertEquals(30, current, 0);
        rotation.target(120);
        rotation.target(-30);
        current = rotation.tick(current);
        assertEquals(10, current, 1e-5);
        current = rotation.tick(current);
        assertEquals(-10, current, 1e-5);
        assertEquals(-30, rotation.tick(current), 1e-5);
    }

    @Test
    public void turnsAcrossBothSidesOfTheAngleBoundaryByTheShortestPath() {
        for (int sign : new int[] { -1, 1 }) {
            CemHeadRotation rotation = new CemHeadRotation();
            float current = sign * 179;
            rotation.target(sign * -179);
            for (int tick = 1; tick <= 3; tick++) {
                current = rotation.tick(current);
                assertEquals(sign * (179 + tick * 2d / 3), current, 2e-5);
            }
            assertEquals(0, MathHelper.wrapAngleTo180_float(current - sign * -179), 2e-5);
        }
    }

    @Test
    public void inactiveStateLeavesSpawnAndSubsequentLocalRotationsAlone() {
        CemHeadRotation rotation = new CemHeadRotation();
        assertEquals(137, rotation.tick(137), 0);
        rotation.target(137);
        for (int tick = 0; tick < 3; tick++) assertEquals(137, rotation.tick(137), 0);
        assertEquals(-62, rotation.tick(-62), 0);
    }
}
