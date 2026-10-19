package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import org.junit.Test;

import industries.angelic.salamander.cem.animation.CemWalkAnimation;

public class CemWalkAnimationTest {

    @Test
    public void swimmingSpeedIsContinuousAtTickBoundariesAndSettlesAfterKnockback() {
        CemWalkAnimation animation = new CemWalkAnimation();
        animation.tick(.25, 0);
        assertEquals(0, animation.speed(0), 0);
        assertEquals(.4, animation.speed(1), 1e-9);
        double previous = animation.speed(1), position = animation.position(1);
        for (int tick = 0; tick < 40; tick++) {
            animation.tick(0, 0);
            assertEquals(previous, animation.speed(0), 1e-9);
            assertEquals(position, animation.position(0), 1e-9);
            assertTrue(animation.speed(1) <= previous);
            assertEquals((previous + animation.speed(1)) / 2, animation.speed(.5f), 1e-9);
            previous = animation.speed(1);
            position = animation.position(1);
        }
        assertEquals(0, animation.speed(1), 1e-8);
    }

    @Test
    public void swimmingUsesHorizontalDisplacementAndCapsSpeed() {
        CemWalkAnimation animation = new CemWalkAnimation();
        for (int tick = 0; tick < 100; tick++) animation.tick(10, -10);
        assertEquals(1, animation.speed(1), 1e-9);
        assertTrue(animation.position(1) > animation.position(0));
    }
}
