package industries.angelic.salamander.skin.expression;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Assume;
import org.junit.Test;

import industries.angelic.salamander.skin.expression.ExpressionDraft.Brows;
import industries.angelic.salamander.skin.expression.ExpressionDraft.Fix;
import industries.angelic.salamander.skin.expression.ExpressionDraft.Pick;
import industries.angelic.salamander.skin.expression.ExpressionDraft.Problem;
import industries.angelic.salamander.skin.expression.ExpressionDraft.Swatch;
import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EditorData;
import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EyeType;

public class ExpressionDraftTest {

    private static final int SKIN = 0xffc08a6a, WHITE = 0xffeeeeee, BLUE = 0xff3344aa, DARK = 0xff2a1a10,
        HAIR = 0xff3c2a14;

    /** A face of skin; pixels set by x, y, mirrored to the other side. */
    private static BufferedImage face() {
        BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) skin.setRGB(8 + x, 8 + y, SKIN);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 2; y++) skin.setRGB(8 + x, 8 + y, HAIR);
        return skin;
    }

    private static void mirrored(BufferedImage skin, int x, int y, int color) {
        skin.setRGB(8 + x, 8 + y, color);
        skin.setRGB(8 + 7 - x, 8 + y, color);
    }

    @Test
    public void findsSteveLikeEyesWithPupilsInside() {
        BufferedImage skin = face();
        mirrored(skin, 1, 4, WHITE);
        mirrored(skin, 2, 4, BLUE);
        ExpressionDetector.Result result = ExpressionDetector.detect(skin);
        assertTrue(result.eyesFound);
        assertEquals(EyeType.NORMAL_2X1, result.eyes);
        assertEquals(4, result.eyeRow);
        assertFalse(ExpressionDraft.outerPupil(result.data, 0));
        assertEquals(BLUE, result.data.pupilColors[2][4]);
        assertEquals(WHITE, result.data.whites[1][4]);
        // Lids take the skin above the eyes.
        assertEquals(SKIN, result.data.lids[1][4]);
        assertNull(result.brows);
        assertTrue(
            ExpressionDraft.problems(skin, result.data)
                .isEmpty());
    }

    @Test
    public void findsTallEyesUnderALashLine() {
        // miku: a dark lash line on row 4, then 2x2 eyes with a white outer and a colored inner column.
        BufferedImage skin = face();
        mirrored(skin, 1, 4, DARK);
        mirrored(skin, 2, 4, DARK);
        mirrored(skin, 1, 5, 0xffb5bcbd);
        mirrored(skin, 2, 5, 0xff39b4be);
        mirrored(skin, 1, 6, 0xffdde4e6);
        mirrored(skin, 2, 6, 0xff49d4e0);
        ExpressionDetector.Result result = ExpressionDetector.detect(skin);
        assertEquals(EyeType.NORMAL_2X2, result.eyes);
        assertEquals(5, result.eyeRow);
        // The lash line above is not skin, so the lids take the face's skin color.
        assertEquals(SKIN, result.data.lids[1][5]);
    }

    @Test
    public void aLashLineAboveAWideEyeIsNotPartOfIt() {
        // alex: a dark line on row 5 over a white and colored 2x1 eye on row 6.
        BufferedImage skin = face();
        mirrored(skin, 1, 5, DARK);
        mirrored(skin, 2, 5, DARK);
        mirrored(skin, 1, 6, WHITE);
        mirrored(skin, 2, 6, 0xff7173b7);
        ExpressionDetector.Result result = ExpressionDetector.detect(skin);
        assertEquals(EyeType.NORMAL_2X1, result.eyes);
        assertEquals(6, result.eyeRow);
    }

    @Test
    public void hairDownTheSidesIsNotAThreeWideEye() {
        BufferedImage skin = face();
        for (int y = 2; y < 8; y++) mirrored(skin, 0, y, HAIR);
        mirrored(skin, 1, 4, WHITE);
        mirrored(skin, 2, 4, BLUE);
        ExpressionDetector.Result result = ExpressionDetector.detect(skin);
        assertEquals(EyeType.NORMAL_2X1, result.eyes);
        assertEquals(4, result.eyeRow);
    }

    @Test
    public void findsPairedAndJoinedBrows() {
        BufferedImage skin = face();
        mirrored(skin, 1, 5, WHITE);
        mirrored(skin, 2, 5, BLUE);
        mirrored(skin, 1, 3, DARK);
        mirrored(skin, 2, 3, DARK);
        ExpressionDetector.Result result = ExpressionDetector.detect(skin);
        assertEquals(5, result.eyeRow);
        assertEquals(Brows.PAIRED, result.brows);
        assertEquals(3, result.browRow);
        assertEquals(DARK, result.data.browColors[1][3]);
        // The cover is the skin next to the brow.
        assertEquals(SKIN, result.data.browBackgrounds[1][3]);
        assertTrue(
            ExpressionDraft.problems(skin, result.data)
                .isEmpty());
        mirrored(skin, 3, 3, DARK);
        result = ExpressionDetector.detect(skin);
        assertEquals(Brows.JOINED, result.brows);
        assertTrue(
            ExpressionDraft.problems(skin, result.data)
                .isEmpty());
    }

    @Test
    public void aFaceWithoutEyesStillGetsADraft() {
        BufferedImage skin = face();
        ExpressionDetector.Result result = ExpressionDetector.detect(skin);
        assertFalse(result.eyesFound);
        assertEquals(EyeType.NORMAL_2X1, ExpressionDraft.shape(result.data, 0));
        assertEquals(4, ExpressionDraft.eyeRow(result.data, 0));
    }

    @Test
    public void vanillaSteveIsFound() throws Exception {
        BufferedImage steve;
        try (InputStream in = getClass().getClassLoader()
            .getResourceAsStream("assets/minecraft/textures/entity/steve.png")) {
            Assume.assumeNotNull(in);
            steve = ImageIO.read(in);
        }
        ExpressionDetector.Result result = ExpressionDetector.detect(steve);
        assertTrue(result.eyesFound);
        assertEquals(EyeType.NORMAL_2X1, result.eyes);
        assertEquals(4, result.eyeRow);
        assertFalse(ExpressionDraft.outerPupil(result.data, 0));
        assertNull(result.brows);
    }

    @Test
    public void handPicksMoveTheRolesWhereTheFormatAllows() {
        BufferedImage skin = face();
        mirrored(skin, 1, 4, WHITE);
        mirrored(skin, 2, 4, BLUE);
        EditorData data = ExpressionDetector.detect(skin).data;
        Boolean[] outer = new Boolean[2];
        // Clicking the white makes it the pupil: the 2x1 eye takes the outer-pupil layout, colors by role.
        assertEquals(Pick.DONE, ExpressionDraft.pick(data, 1, 4, outer));
        assertEquals(Boolean.TRUE, outer[0]);
        ExpressionDraft.placeEyes(data, skin, EyeType.NORMAL_2X1, 4, 0, outer[0]);
        assertTrue(ExpressionDraft.outerPupil(data, 0));
        assertFalse(ExpressionDraft.outerPupil(data, 1));
        assertTrue(data.pupils[1][4]);
        assertEquals(WHITE, data.pupilColors[1][4]);
        assertEquals(BLUE, data.whites[2][4]);
        roundTrip(skin, data);
        // Clicking the pupil makes it white again: the pupil goes back inside.
        assertEquals(Pick.DONE, ExpressionDraft.pick(data, 1, 4, outer));
        assertEquals(Boolean.FALSE, outer[0]);
        // A hand pick beats what the skin shows: outer pupils on a skin whose pupils are inside.
        outer[1] = true;
        ExpressionDraft.placeEyes(data, skin, EyeType.NORMAL_2X1, 5, 1, outer[1]);
        assertTrue(ExpressionDraft.outerPupil(data, 1));
        assertEquals(5, ExpressionDraft.eyeRow(data, 1));
    }

    @Test
    public void picksTheFormatCannotStoreLeaveTheEyeAsItWas() {
        BufferedImage skin = face();
        EditorData data = new EditorData();
        Boolean[] outer = new Boolean[2];
        ExpressionDraft.placeEyes(data, skin, EyeType.NORMAL_2X2, 4, 0, null);
        EditorData before = data.copy();
        assertEquals(Pick.FIXED_SHAPE, ExpressionDraft.pick(data, 1, 4, outer));
        assertNull(outer[0]);
        assertArrayEquals(before.pupils[1], data.pupils[1]);
        ExpressionDraft.placeEyes(data, skin, EyeType.SIMPLE_1X1, 4, 0, null);
        assertEquals(Pick.NO_PUPIL, ExpressionDraft.pick(data, 2, 4, outer));
        assertEquals(Pick.NOT_AN_EYE, ExpressionDraft.pick(data, 0, 0, outer));
        // A pick held for 2x1 eyes is ignored by shapes that can't store it.
        ExpressionDraft.placeEyes(data, skin, EyeType.NORMAL_2X2, 4, 0, true);
        assertFalse(ExpressionDraft.outerPupil(data, 0));
        assertTrue(data.pupils[2][4]);
    }

    @Test
    public void swatchesColorAWholeFeature() {
        BufferedImage skin = face();
        EditorData data = new EditorData();
        ExpressionDraft.placeEyes(data, skin, EyeType.NORMAL_2X2, 4, 0, null);
        ExpressionDraft.paint(data, Swatch.WHITE, 0, WHITE);
        ExpressionDraft.paint(data, Swatch.PUPIL, 0, BLUE);
        ExpressionDraft.paint(data, Swatch.LID, 0, DARK);
        assertArrayEquals(new int[] { WHITE }, ExpressionDraft.colors(data, Swatch.WHITE, 0));
        assertArrayEquals(new int[] { BLUE }, ExpressionDraft.colors(data, Swatch.PUPIL, 0));
        assertEquals(DARK, data.lids[2][5]);
        assertEquals(0, ExpressionDraft.colors(data, Swatch.WHITE, 1).length);
        // Advanced painting gives a lid column its own color.
        ExpressionDraft.paintPixel(data, Swatch.LID, 1, 5, SKIN);
        assertEquals(SKIN, data.lids[1][4]);
        assertEquals(DARK, data.lids[2][4]);
        assertEquals(2, ExpressionDraft.colors(data, Swatch.LID, 0).length);
    }

    @Test
    public void theHatOverTheEyesMovesOntoTheFaceLookingTheSame() {
        BufferedImage skin = face();
        mirrored(skin, 1, 4, WHITE);
        mirrored(skin, 2, 4, BLUE);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) skin.setRGB(40 + x, 8 + y, 0x6e50c8ff);
        EditorData data = ExpressionDetector.detect(skin).data;
        List<Problem> problems = ExpressionDraft.problems(skin, data);
        assertEquals(1, problems.size());
        assertEquals("hatOverEyes", problems.get(0).key);
        assertEquals(Fix.MOVE_HAT_TO_FACE, problems.get(0).fix);
        BufferedImage fixed = ExpressionDraft.fix(Fix.MOVE_HAT_TO_FACE, skin, data);
        assertNotSame(skin, fixed);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) {
            int seen = ExpressionDraft.over(skin.getRGB(40 + x, 8 + y), skin.getRGB(8 + x, 8 + y));
            int now = ExpressionDraft.over(fixed.getRGB(40 + x, 8 + y), fixed.getRGB(8 + x, 8 + y));
            assertEquals(x + "," + y, seen, now);
        }
        assertEquals(0, fixed.getRGB(40 + 1, 8 + 4));
        assertEquals(0x6e50c8ff, fixed.getRGB(40 + 1, 8 + 3));
        assertTrue(
            ExpressionDraft.problems(fixed, data)
                .isEmpty());
    }

    @Test
    public void eyelashesNeedBottomTwoByTwoEyes() {
        BufferedImage skin = face();
        EditorData data = new EditorData();
        for (int side = 0; side < 2; side++) ExpressionDraft.placeEyes(data, skin, EyeType.NORMAL_2X2, 5, side, null);
        assertTrue(ExpressionDraft.lashesFit(data, 0));
        ExpressionDraft.setLashes(data, 0, true);
        assertTrue(
            ExpressionDraft.problems(skin, data)
                .isEmpty());
        roundTrip(skin, data);
        ExpressionDraft.placeEyes(data, skin, EyeType.NORMAL_2X1, 4, 0, null);
        List<Problem> problems = ExpressionDraft.problems(skin, data);
        assertEquals("lashes", problems.get(0).key);
        ExpressionDraft.fix(problems.get(0).fix, skin, data);
        assertFalse(ExpressionDraft.hasLashes(data, 0));
        assertTrue(
            ExpressionDraft.problems(skin, data)
                .isEmpty());
    }

    @Test
    public void paintedLidsAndJoinedBrowsGetOneClickFixes() {
        BufferedImage skin = face();
        EditorData data = new EditorData();
        for (int side = 0; side < 2; side++) ExpressionDraft.placeEyes(data, skin, EyeType.NORMAL_2X2, 4, side, null);
        data.lids[1][5] = BLUE;
        ExpressionDraft.placeBrows(data, skin, Brows.JOINED, 3, -1);
        data.browColors[4][3] = BLUE;
        List<Problem> problems = ExpressionDraft.problems(skin, data);
        assertEquals("lidColumns", problems.get(0).key);
        assertEquals("joinedBrow", problems.get(1).key);
        for (Problem problem : problems) ExpressionDraft.fix(problem.fix, skin, data);
        assertTrue(
            ExpressionDraft.problems(skin, data)
                .isEmpty());
        roundTrip(skin, data);
    }

    @Test
    public void theFaceMovesAsThePacksDo() {
        BufferedImage skin = face();
        mirrored(skin, 1, 4, WHITE);
        mirrored(skin, 2, 4, BLUE);
        mirrored(skin, 1, 2, DARK);
        mirrored(skin, 2, 2, DARK);
        EditorData data = ExpressionDetector.detect(skin).data;
        int[][] rest = ExpressionFace.colors(skin, data, 0, 0, 0, 0, false);
        assertEquals(WHITE, rest[1][4]);
        assertEquals(BLUE, rest[2][4]);
        // Looking right: the left eye's pupil is already at its right, the right eye's moves over.
        int[][] right = ExpressionFace.colors(skin, data, 1, 0, 0, 0, false);
        assertEquals(BLUE, right[2][4]);
        assertEquals(WHITE, right[5][4]);
        assertEquals(BLUE, right[6][4]);
        int[][] closed = ExpressionFace.colors(skin, data, 0, 0, 1, 0, false);
        assertEquals(data.lids[1][4], closed[1][4]);
        assertEquals(data.lids[2][4], closed[2][4]);
        if (data.brows[1][2] != 0) {
            int[][] raised = ExpressionFace.colors(skin, data, 0, 0, 0, 1, false);
            assertEquals(DARK, raised[1][1]);
            assertEquals(data.browBackgrounds[1][2], raised[1][2]);
        }
    }

    private static void roundTrip(BufferedImage skin, EditorData data) {
        BufferedImage out = ExpressionSkinExporter.export(skin, data);
        EditorData loaded = ExpressionSkinExporter.readEditorData(out);
        assertNotNull(loaded);
        BufferedImage again = ExpressionSkinExporter.export(out, loaded);
        for (int x = 0; x < 64; x++) for (int y = 0; y < 64; y++) assertEquals(out.getRGB(x, y), again.getRGB(x, y));
    }

    @Test
    public void oldSkinsBecome64x64WithOneClick() {
        BufferedImage legacy = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 64; x++) for (int y = 16; y < 32; y++) legacy.setRGB(x, y, 0xff000000 | x << 8 | y);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) legacy.setRGB(8 + x, 8 + y, SKIN);
        mirrored(legacy, 1, 4, WHITE);
        mirrored(legacy, 2, 4, BLUE);
        EditorData data = ExpressionDetector.detect(legacy).data;
        List<Problem> problems = ExpressionDraft.problems(legacy, data);
        assertEquals("legacy", problems.get(0).key);
        assertEquals(Fix.CONVERT_LEGACY, problems.get(0).fix);
        BufferedImage modern = ExpressionDraft.fix(Fix.CONVERT_LEGACY, legacy, data);
        assertEquals(64, modern.getHeight());
        // The face stays, and the left leg and arm now have pixels, as 1.8 copies them from the right ones.
        assertEquals(WHITE, modern.getRGB(9, 12));
        assertNotEquals(0, modern.getRGB(20, 52) >>> 24);
        assertNotEquals(0, modern.getRGB(36, 52) >>> 24);
        assertTrue(
            ExpressionDraft.problems(modern, data)
                .isEmpty());
        roundTrip(modern, data);
    }
}
