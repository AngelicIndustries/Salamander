package industries.angelic.salamander.skin.layers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import industries.angelic.salamander.api.skin.SkinPart;

public class CoverageTest {

    @Test
    public void aGloveCoversTheLowerSleeveAndItsEnd() {
        // The Aether's gloves: the arm's lowest five rows all around, and its bottom.
        Coverage.Texels glove = (u, v) -> u >= 40 && u < 56 && v >= 27 && v < 32
            || u >= 48 && u < 52 && v < 20 && v >= 16;
        Coverage.Mask mask = mask(SkinPart.RIGHT_SLEEVE, Coverage.Box.ARM, glove, 40, 32, 4);

        for (int u = 40; u < 56; u++) {
            for (int v = 36; v < 48; v++) {
                assertEquals(u + "," + v, v >= 43, mask.covers(u, v));
            }
        }
        for (int u = 48; u < 52; u++) assertTrue(mask.covers(u, 33));
        for (int u = 44; u < 48; u++) assertFalse(mask.covers(u, 33));
    }

    @Test
    public void leftLimbsWearTheRightLimbsTextureMirrored() {
        // The x min side's first column, for the right arm the outer side's front edge.
        Coverage.Mask mask = mask(SkinPart.LEFT_SLEEVE, Coverage.Box.ARM, (u, v) -> u == 40 && v == 20, 48, 48, 4);

        // As a legacy skin's left arm is converted: the x max side's last column.
        assertTrue(mask.covers(59, 52));
        assertEquals(1, covered(mask, 48, 48, 16, 16));
    }

    @Test
    public void slimArmsLackTheOuterColumn() {
        Coverage.Mask outer = mask(SkinPart.RIGHT_SLEEVE, Coverage.Box.ARM, (u, v) -> u == 44 && v == 20, 40, 32, 3);
        assertSame(Coverage.Mask.NONE, outer);

        Coverage.Mask next = mask(SkinPart.RIGHT_SLEEVE, Coverage.Box.ARM, (u, v) -> u == 45 && v == 20, 40, 32, 3);
        assertTrue(next.covers(44, 36));
        assertEquals(1, covered(next, 40, 32, 14, 16));

        // On the left arm the outer column is the last of the front, the first of the back.
        Coverage.Mask left = mask(SkinPart.LEFT_SLEEVE, Coverage.Box.ARM, (u, v) -> u == 44 && v == 20, 48, 48, 3);
        assertSame(Coverage.Mask.NONE, left);
    }

    @Test
    public void theHelmetsHeadwearCoversTheHat() {
        List<Coverage.Textured> helmet = Arrays.asList(
            new Coverage.Textured(null, Coverage.Box.HEAD, (u, v) -> false),
            new Coverage.Textured(null, Coverage.Box.HEADWEAR, (u, v) -> u == 40 && v == 8));
        Coverage.Mask mask = Coverage.mask(SkinPart.HAT, helmet, 32, 0, 8, 8, 8);

        assertTrue(mask.covers(40, 8));
        assertEquals(1, covered(mask, 32, 0, 32, 16));
    }

    @Test
    public void wholeAndNothing() {
        assertSame(Coverage.Mask.ALL, mask(SkinPart.RIGHT_PANTS, Coverage.Box.LEG, (u, v) -> true, 0, 32, 4));
        assertSame(Coverage.Mask.NONE, mask(SkinPart.RIGHT_PANTS, Coverage.Box.LEG, (u, v) -> false, 0, 32, 4));
        assertTrue(Coverage.Mask.ALL.covers(5, 5));
        assertFalse(Coverage.Mask.NONE.covers(5, 5));
    }

    @Test
    public void boxesOfAnotherShapeAreNotMapped() {
        List<Coverage.Textured> chest = Collections
            .singletonList(new Coverage.Textured(null, Coverage.Box.BODY, (u, v) -> false));
        assertSame(Coverage.Mask.UNMAPPED, Coverage.mask(SkinPart.JACKET, chest, 16, 32, 8, 10, 4));
    }

    @Test
    public void textureTexelsDrawAboveTheAlphaTestAtAnyResolution() {
        BufferedImage image = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(3, 2, 0x1A000000);
        image.setRGB(4, 4, 0x19000000);
        Coverage.Texels texels = Coverage.Opacity.of(image);

        assertTrue(texels.drawn(1, 1));
        assertFalse(texels.drawn(2, 2));
        assertFalse(texels.drawn(0, 0));
    }

    private static Coverage.Mask mask(SkinPart part, Coverage.Box box, Coverage.Texels texels, int u, int v,
        int width) {
        return Coverage.mask(
            part,
            Collections.singletonList(new Coverage.Textured(null, box, texels)),
            u,
            v,
            width,
            box.height,
            box.depth);
    }

    private static int covered(Coverage.Mask mask, int u, int v, int width, int height) {
        int count = 0;
        for (int x = u; x < u + width; x++) {
            for (int y = v; y < v + height; y++) {
                if (mask.covers(x, y)) count++;
            }
        }
        return count;
    }
}
