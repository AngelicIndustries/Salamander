package industries.angelic.salamander.skin.expression;

import static org.junit.Assert.*;

import org.junit.Test;

import industries.angelic.salamander.skin.expression.FaceIcon.Eyes;

public class FaceIconTest {

    @Test
    public void notHoveredItLooksAhead() {
        assertEquals(Eyes.AHEAD, FaceIcon.eyes(-1L));
    }

    @Test
    public void hoveredEyesBlinkThenGlanceBothWays() {
        assertEquals(Eyes.CLOSED, FaceIcon.eyes(0L));
        assertEquals(Eyes.AHEAD, FaceIcon.eyes(400L));
        assertEquals(Eyes.LEFT, FaceIcon.eyes(1000L));
        assertEquals(Eyes.RIGHT, FaceIcon.eyes(1600L));
        assertEquals(Eyes.AHEAD, FaceIcon.eyes(2500L));
        assertEquals(Eyes.CLOSED, FaceIcon.eyes(2800L));
    }

    @Test
    public void bothEyesShiftTogetherWhenGlancing() {
        // Two-texel eyes a texel in from the outline: texels 1-2 and 5-6, a texel further left or right.
        assertArrayEquals(new int[] { 1, 5 }, FaceIcon.eyeColumns(Eyes.AHEAD));
        assertArrayEquals(new int[] { 0, 4 }, FaceIcon.eyeColumns(Eyes.LEFT));
        assertArrayEquals(new int[] { 2, 6 }, FaceIcon.eyeColumns(Eyes.RIGHT));
    }

    @Test
    public void theFaceFillsItsBoxInWholeScreenPixels() {
        // Wawel Auth's menu icons are 12 GUI pixels: 3 screen pixels a texel at GUI scale 2, filling the box.
        float[] fit = FaceIcon.fit(10, 20, 12, 12, 2);
        assertEquals(1.5f, fit[2], 0);
        assertEquals(10, fit[0], 0);
        assertEquals(20, fit[1], 0);
        assertEquals(0.5f, fit[3], 0);
        // At scale 3, 4 screen pixels a texel, centered in what is left.
        fit = FaceIcon.fit(0, 0, 12, 12, 3);
        assertEquals(4 / 3.0f, fit[2], 1e-6);
        assertEquals((12 - 32 / 3.0f) / 2, fit[0], 1e-6);
        // An 8 pixel box at scale 1 still gets a texel per pixel, and a 24 pixel button 3 GUI pixels.
        assertEquals(1, FaceIcon.fit(0, 0, 8, 8, 1)[2], 0);
        assertEquals(3, FaceIcon.fit(0, 0, 24, 24, 2)[2], 0);
    }

    @Test
    public void colorsAreWawelAuthsHeadTinted() {
        assertEquals(FaceIcon.OUTLINE, FaceIcon.tinted(FaceIcon.OUTLINE, 0xFFFFFFFF));
        assertEquals(0xFF9C9CA1, FaceIcon.tinted(FaceIcon.FILL, 0xFFFFFFFF));
        assertEquals(0xFFA6A6A6, FaceIcon.tinted(FaceIcon.OUTLINE, 0xFFB8B8B8));
    }
}
