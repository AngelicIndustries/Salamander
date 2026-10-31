package industries.angelic.salamander.skin.layers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import industries.angelic.salamander.api.skin.LayerSegment;
import industries.angelic.salamander.api.skin.SkinPart;

public class OverlayBoxesTest {

    @Test
    public void aFlatSleeveKeepsTheRowsAboveAGlove() {
        // The glove covers the arm's lowest five rows all around and its bottom.
        Coverage.Mask glove = Coverage.mask(
            SkinPart.RIGHT_SLEEVE,
            Collections.singletonList(
                new Coverage.Textured(
                    null,
                    Coverage.Box.ARM,
                    (u, v) -> u >= 40 && u < 56 && v >= 27 && v < 32 || u >= 48 && u < 52 && v >= 16 && v < 20)),
            40,
            32,
            4,
            12,
            4);
        LayerSegment sleeve = LayerSegment.box(SkinPart.RIGHT_SLEEVE, -3, -2, -2, 4, 12, 4, 40, 32);
        List<OverlayBoxGeometry.Quad> faces = OverlayBoxGeometry.quads(sleeve, 0.25F);

        // Each side keeps one rectangle, its upper seven rows of twelve.
        for (int face : new int[] { 0, 1, 4, 5 }) {
            List<float[]> open = OverlayBoxes.open(faces.get(face), glove);
            assertEquals(1, open.size());
            assertArrayEquals(new float[] { 0, 1, 0, 7 / 12.0F }, open.get(0), 1.0E-6F);
        }
        // The top stays whole, the bottom goes.
        assertArrayEquals(
            new float[] { 0, 1, 0, 1 },
            OverlayBoxes.open(faces.get(2), glove)
                .get(0),
            1.0E-6F);
        assertEquals(
            0,
            OverlayBoxes.open(faces.get(3), glove)
                .size());
    }
}
