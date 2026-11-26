package industries.angelic.salamander.skin.expression;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;

import org.junit.Test;

import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EditorData;
import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EyeType;

public class ExpressionSkinExporterTest {

    private final BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    private EditorData data = new EditorData();

    private BufferedImage export() {
        return ExpressionSkinExporter.export(skin, data);
    }

    private void select(EyeType type, int row) {
        data = new EditorData();
        for (int side = 0; side < 2; side++) ExpressionSkinExporter.eyePreset(data, skin, type, row, side);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) {
            data.lids[x][y] = 0xffaabb00 + x;
            data.whites[x][y] = 0xffeeee00 + y;
            data.pupilColors[x][y] = 0xff00aa00 + x * 16 + y;
        }
    }

    private EditorData roundTrip(BufferedImage out) {
        EditorData loaded = ExpressionSkinExporter.readEditorData(out);
        assertNotNull("Exported features should reopen", loaded);
        BufferedImage again = ExpressionSkinExporter.export(out, loaded);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++) assertEquals(x + "," + y, out.getRGB(x, y), again.getRGB(x, y));
        return loaded;
    }

    @Test
    public void standardEyesUseTheDocumentedSharedSlotsAndRoundTrip() {
        int[][] layouts = { { 1, 6, 4, 6 }, { 1, 5, 4, 4 }, { 1, 4, 4, 2 }, { 1, 3, 4, 0 }, { 2, 5, 24, 5 },
            { 2, 4, 24, 2 }, { 2, 3, 28, 5 }, { 2, 2, 28, 2 }, { 3, 4, 32, 4 } };
        for (int[] layout : layouts) {
            select(EyeType.values()[layout[0] - 1], layout[1]);
            BufferedImage out = export();
            int u = layout[2], v = layout[3], y = layout[1];
            assertEquals(data.lids[1][y], out.getRGB(u, v));
            assertEquals(data.lids[6][y], out.getRGB(u + 3, v));
            assertEquals(data.whites[1][y], out.getRGB(u, v + 1));
            assertEquals(data.pupilColors[2][y], out.getRGB(u + 1, v + 1));
            assertEquals(data.pupilColors[5][y], out.getRGB(u + 2, v + 1));
            assertEquals(data.whites[6][y], out.getRGB(u + 3, v + 1));
            EditorData loaded = roundTrip(out);
            for (int x = 0; x < 8; x++) for (int row = 0; row < 8; row++) {
                assertEquals(data.eyes[x][row], loaded.eyes[x][row]);
                assertEquals(data.pupils[x][row], loaded.pupils[x][row]);
            }
        }
    }

    @Test
    public void everyPresetAndHeightCanBeExportedAndReopened() {
        for (EyeType type : EyeType.values()) for (int row = type.minRow; row <= type.maxRow; row++) {
            select(type, row);
            roundTrip(export());
        }
    }

    @Test
    public void derpEndermanAndWideEyesUseTheirOwnPupilsAndWhites() {
        select(EyeType.DERP, 5);
        BufferedImage out = export();
        assertEquals(data.pupilColors[1][5], out.getRGB(2, 5));
        assertEquals(data.pupilColors[6][5], out.getRGB(3, 5));
        assertEquals(0, out.getRGB(5, 5));
        assertEquals(0, out.getRGB(6, 5));
        select(EyeType.ENDERMAN, 4);
        for (int x : new int[] { 0, 1, 2, 5, 6, 7 }) data.whites[x][4] = 0xffaabb00 + x;
        out = export();
        for (int x = 0; x < 3; x++) {
            assertEquals(data.whites[x][4], out.getRGB(32 + x, 1));
            assertEquals(data.whites[5 + x][4], out.getRGB(37 + x, 1));
        }
        assertEquals(data.pupilColors[1][4], out.getRGB(35, 1));
        assertEquals(data.pupilColors[6][4], out.getRGB(36, 1));
        roundTrip(out);
        select(EyeType.WIDE, 5);
        out = export();
        assertEquals(data.lids[0][5], out.getRGB(36, 6));
        assertEquals(data.pupilColors[1][5], out.getRGB(37, 7));
        assertEquals(data.pupilColors[6][5], out.getRGB(38, 7));
        roundTrip(out);
    }

    @Test
    public void simpleEyesUseOnlyTheirEyelidTexel() {
        int[][] simple = { { 1, 4, 1, 7 }, { 1, 3, 1, 6 }, { 2, 4, 1, 5 }, { 2, 3, 1, 4 }, { 2, 4, 2, 3 },
            { 2, 3, 2, 2 }, { 2, 3, 3, 1 }, { 2, 2, 3, 0 } };
        for (int[] layout : simple) {
            EyeType type = layout[0] == 1 ? EyeType.SIMPLE_WIDE
                : layout[2] == 1 ? EyeType.SIMPLE_1X1 : layout[2] == 2 ? EyeType.SIMPLE_1X2 : EyeType.SIMPLE_1X3;
            select(type, layout[1]);
            BufferedImage out = export();
            assertEquals(data.lids[layout[0]][layout[1]], out.getRGB(0, layout[3]));
            assertEquals(data.lids[7 - layout[0]][layout[1]], out.getRGB(1, layout[3]));
            assertEquals(0, out.getRGB(4, 0));
            roundTrip(out);
        }
    }

    private void brow(int x, int y) {
        data.brows[x][y] = (byte) (x < 4 ? 1 : 2);
        data.browColors[x][y] = 0xff765400 + x;
        data.browBackgrounds[x][y] = 0xffaabb00 + x;
    }

    @Test
    public void removingBrowsClearsBothSidesAndLayersWithoutChangingEyes() {
        select(EyeType.NORMAL_2X2, 5);
        for (int x : new int[] { 1, 2, 5, 6 }) {
            brow(x, 3);
            data.overlayBrows[x][2] = (byte) (x < 4 ? 1 : 2);
            data.overlayColors[x][2] = 0xff112233;
        }
        BufferedImage before = export();
        data = ExpressionSkinExporter.readEditorData(before);
        assertNotNull(data);
        EditorData saved = data.copy();
        data.clearBrows();
        BufferedImage out = ExpressionSkinExporter.export(before, data);
        for (int x = 56; x < 64; x++)
            for (int y = 0; y < 8; y++) assertEquals("No brow texel should survive removal", 0, out.getRGB(x, y));
        for (int x = 24; x < 28; x++) for (int y = 5; y < 8; y++)
            assertEquals("Keep the user's eyes and colors", before.getRGB(x, y), out.getRGB(x, y));
        roundTrip(out);
        data.copyFrom(saved);
        assertEquals("Undo restores eyebrows", before.getRGB(60, 3), export().getRGB(60, 3));
    }

    @Test
    public void removingTheLastFeatureExportsACleanSkin() {
        for (int x = 1; x <= 6; x++) brow(x, 3);
        data.browColors[4][3] = data.browColors[3][3];
        data.browBackgrounds[4][3] = data.browBackgrounds[3][3];
        BufferedImage before = export();
        data.clearBrows();
        BufferedImage out = ExpressionSkinExporter.export(before, data);
        for (int x = 0; x < 64; x++) for (int y = 0; y < 8; y++) assertEquals(0, out.getRGB(x, y));
        assertNull("No expression features remain", ExpressionSkinExporter.readEditorData(out));
    }

    @Test
    public void eyebrowsCanMoveIndependentlyOfTheEyesAndRoundTripTheirColors() {
        for (int row = 2; row <= 5; row++) {
            select(EyeType.NORMAL_2X1, 6);
            for (int x : new int[] { 1, 2, 5, 6 }) brow(x, row);
            BufferedImage out = export();
            assertEquals(data.browBackgrounds[1][row], out.getRGB(60, (row - 2) * 2));
            assertEquals(data.browColors[6][row], out.getRGB(63, (row - 2) * 2 + 1));
            roundTrip(out);
        }
    }

    @Test
    public void joinedAndWideBrowsUseTheirSharedPackSlots() {
        for (int x = 1; x <= 6; x++) brow(x, 3);
        data.browColors[4][3] = data.browColors[3][3];
        data.browBackgrounds[4][3] = data.browBackgrounds[3][3];
        BufferedImage out = export();
        assertEquals(data.browBackgrounds[3][3], out.getRGB(26, 0));
        assertEquals(data.browColors[6][3], out.getRGB(28, 1));
        roundTrip(out);
        data.browColors[4][3] = 0xff123456;
        assertThrows(IllegalArgumentException.class, this::export);
        data = new EditorData();
        for (int x : new int[] { 0, 1, 6, 7 }) brow(x, 3);
        out = export();
        assertEquals(data.browBackgrounds[0][3], out.getRGB(36, 4));
        assertEquals(data.browColors[7][3], out.getRGB(39, 5));
        roundTrip(out);
    }

    @Test
    public void overlayBrowsAndLashesRetainTransparencyAndUseDedicatedSlots() {
        select(EyeType.NORMAL_2X2, 5);
        for (int x : new int[] { 1, 2, 5, 6 }) {
            data.overlayBrows[x][3] = (byte) (x < 4 ? 1 : 2);
            data.overlayColors[x][3] = 0x80765400 + x;
            data.lashes[x][4] = 0xff334400 + x;
        }
        data.lashes[0][5] = 0xff123456;
        data.lashes[7][6] = 0xff654321;
        BufferedImage out = export();
        assertEquals(data.overlayColors[1][3], out.getRGB(56, 5));
        assertEquals(data.overlayColors[6][3], out.getRGB(59, 5));
        assertEquals(data.lashes[1][4], out.getRGB(56, 0));
        assertEquals(data.lashes[6][4], out.getRGB(59, 0));
        assertEquals(data.lashes[0][5], out.getRGB(56, 1));
        assertEquals(data.lashes[7][6], out.getRGB(59, 2));
        roundTrip(out);
        ExpressionSkinExporter.eyePreset(data, skin, EyeType.NORMAL_2X1, 6, 0);
        assertThrows(IllegalArgumentException.class, this::export);
    }

    @Test
    public void visibleSkinIsUntouchedAndIncompatibleCornerDataIsDiscarded() {
        for (int x = 0; x < 64; x++) for (int y = 0; y < 64; y++) skin.setRGB(x, y, 0xff000000 | x * 255 + y);
        for (int x = 40; x < 48; x++) for (int y = 8; y < 16; y++) skin.setRGB(x, y, 0);
        assertNull("Opaque unrelated metadata is not a prepared skin", ExpressionSkinExporter.readEditorData(skin));
        select(EyeType.NORMAL_2X2, 5);
        BufferedImage out = export();
        for (int x = 0; x < 64; x++) for (int y = 0; y < 64; y++) {
            if (y < 8 && (x < 8 || x >= 24 && x < 40 || x >= 56)) continue;
            assertEquals(x + "," + y, skin.getRGB(x, y), out.getRGB(x, y));
        }
        assertEquals("Old black eyebrow texels must not survive", 0, out.getRGB(60, 7));
        roundTrip(out);
        out.setRGB(0, 0, 0xff000000);
        assertNull("Conflicting simple eye metadata must be rejected", ExpressionSkinExporter.readEditorData(out));
    }

    @Test
    public void refusesLossySelectionsInsteadOfInventingAWorkingPackSkin() {
        select(EyeType.NORMAL_2X1, 6);
        data.eyes[3][6] = 1;
        assertThrows(IllegalArgumentException.class, this::export);
        data.eyes[3][6] = 0;
        data.whites[2][6] = 0xff123456;
        assertThrows(IllegalArgumentException.class, this::export);
        data.whites[2][6] = data.whites[1][6];
        skin.setRGB(41, 14, 0xff123456);
        assertThrows(IllegalArgumentException.class, this::export);
        assertThrows(
            IllegalArgumentException.class,
            () -> ExpressionSkinExporter.export(new BufferedImage(128, 128, 2), data));
        skin.setRGB(41, 14, 0);
        select(EyeType.NORMAL_2X2, 5);
        data.lids[1][6] = 0xffabcdef;
        assertThrows(IllegalArgumentException.class, this::export);
    }

    @Test
    public void snapshotsAreIndependentAndRestoreAllFeatures() {
        select(EyeType.NORMAL_2X2, 5);
        brow(1, 3);
        data.overlayBrows[5][3] = 2;
        data.overlayColors[5][3] = 0x80776655;
        data.lashes[0][5] = 0xff112233;
        EditorData saved = data.copy();
        data.copyFrom(new EditorData());
        assertEquals(0, data.eyes[1][5]);
        data.copyFrom(saved);
        assertEquals(1, data.eyes[1][5]);
        assertEquals(0x80776655, data.overlayColors[5][3]);
        assertEquals(0xff112233, data.lashes[0][5]);
        data.lids[1][5] = 0;
        assertNotEquals(0, saved.lids[1][5]);
    }

    private static final int WHITE = 0xffeeeeee, BLUE = 0xff3344aa, SKIN = 0xffc08a6a;

    /** A face of skin with one row of eye pixels, left to right. */
    private void face(int row, int... eyeRow) {
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) skin.setRGB(8 + x, 8 + y, SKIN);
        for (int x = 0; x < 8; x++) if (eyeRow[x] != 0) skin.setRGB(8 + x, 8 + row, eyeRow[x]);
    }

    @Test
    public void pupilsAndWhitesComeFromTheSkinNotFromTheirSlots() {
        // Pupils inside, as Steve's: the format's own layout.
        face(4, SKIN, WHITE, BLUE, SKIN, SKIN, BLUE, WHITE, SKIN);
        data = new EditorData();
        for (int side = 0; side < 2; side++) ExpressionSkinExporter.eyePreset(data, skin, EyeType.NORMAL_2X1, 4, side);
        assertEquals(EyeType.NORMAL_2X1, ExpressionSkinExporter.eyeType(data, 0));
        assertTrue(data.pupils[2][4] && data.pupils[5][4]);
        assertEquals(BLUE, data.pupilColors[2][4]);
        assertEquals(WHITE, data.whites[1][4]);
        // Pupils outside: 2x1 eyes take the outer-pupil layout, with the same colors.
        face(4, SKIN, BLUE, WHITE, SKIN, SKIN, WHITE, BLUE, SKIN);
        data = new EditorData();
        for (int side = 0; side < 2; side++) ExpressionSkinExporter.eyePreset(data, skin, EyeType.NORMAL_2X1, 4, side);
        assertEquals(EyeType.DERP, ExpressionSkinExporter.eyeType(data, 0));
        assertEquals(EyeType.DERP, ExpressionSkinExporter.eyeType(data, 1));
        assertTrue(data.pupils[1][4] && data.pupils[6][4]);
        assertEquals(BLUE, data.pupilColors[1][4]);
        assertEquals(BLUE, data.pupilColors[6][4]);
        assertEquals(WHITE, data.whites[2][4]);
        BufferedImage out = export();
        // Outer pupils of 2x1 eyes at row 4 sit at (2, 3) and (3, 3).
        assertEquals(BLUE, out.getRGB(2, 3));
        assertEquals(BLUE, out.getRGB(3, 3));
        roundTrip(out);
        // Taller eyes keep the format's inner pupil, but still in the pupil's color, never the white's.
        face(4, SKIN, BLUE, WHITE, SKIN, SKIN, WHITE, BLUE, SKIN);
        for (int x : new int[] { 1, 6 }) skin.setRGB(8 + x, 13, BLUE);
        for (int x : new int[] { 2, 5 }) skin.setRGB(8 + x, 13, WHITE);
        data = new EditorData();
        for (int side = 0; side < 2; side++) ExpressionSkinExporter.eyePreset(data, skin, EyeType.NORMAL_2X2, 4, side);
        assertTrue(data.pupils[2][4]);
        assertEquals(BLUE, data.pupilColors[2][4]);
        assertEquals(BLUE, data.pupilColors[2][5]);
        assertEquals(WHITE, data.whites[1][4]);
        assertEquals(WHITE, data.whites[2][4]);
        roundTrip(export());
    }

    @Test
    public void vanillaSteveKeepsHisPupilsInside() throws Exception {
        java.io.InputStream in = getClass().getClassLoader()
            .getResourceAsStream("assets/minecraft/textures/entity/steve.png");
        org.junit.Assume.assumeNotNull(in);
        BufferedImage steve = javax.imageio.ImageIO.read(in);
        data = new EditorData();
        for (int side = 0; side < 2; side++) ExpressionSkinExporter.eyePreset(data, steve, EyeType.NORMAL_2X1, 4, side);
        assertEquals(EyeType.NORMAL_2X1, ExpressionSkinExporter.eyeType(data, 0));
        assertEquals(EyeType.NORMAL_2X1, ExpressionSkinExporter.eyeType(data, 1));
        assertEquals(0xffffffff, data.whites[1][4]);
        assertEquals(steve.getRGB(10, 12), data.pupilColors[2][4]);
    }
}
