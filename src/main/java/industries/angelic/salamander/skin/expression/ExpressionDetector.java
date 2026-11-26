package industries.angelic.salamander.skin.expression;

import java.awt.image.BufferedImage;

import industries.angelic.salamander.skin.expression.ExpressionDraft.Brows;
import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EditorData;
import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EyeType;

/**
 * The first draft for a skin no pack setup was found in: where its eyes are and what shape, which side its pupils are
 * on, its brows and its lids, read from the base layer of the face, where the packs need the eyes.
 * <p>
 * Every shape and row the packs support is scored by how much its pixels look like eyes: unlike the skin, with a white
 * or a light and dark pair in a row, and not part of a strand of hair, a mouth or a brow above. The best one wins;
 * below {@link #FOUND} the eyes count as not found and the draft keeps 2x1 eyes on row 4.
 */
final class ExpressionDetector {

    /** What was found, and the draft made from it. */
    static final class Result {

        final EditorData data = new EditorData();
        EyeType eyes = EyeType.NORMAL_2X1;
        int eyeRow = 4;
        boolean eyesFound;
        Brows brows;
        int browRow = -1;
    }

    /** How different from the skin a pixel must be to count. */
    private static final int THRESHOLD = 48;
    private static final int CAP = 120;
    /** The score below which no eyes count as found. */
    static final double FOUND = 40;

    private ExpressionDetector() {}

    static Result detect(BufferedImage skin) {
        Result result = new Result();
        if (!ExpressionDraft.usable(skin)) return result;
        int[][] face = new int[8][8];
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) face[x][y] = ExpressionSkinExporter.base(skin, x, y);
        int skinColor = ExpressionDraft.skinColor(skin);
        double best = Double.NEGATIVE_INFINITY;
        for (EyeType type : ExpressionDraft.SHAPES) for (int row = type.minRow; row <= type.maxRow; row++) {
            double score = score(face, skinColor, type, row);
            if (score > best) {
                best = score;
                result.eyes = type;
                result.eyeRow = row;
            }
        }
        result.eyesFound = best >= FOUND;
        if (!result.eyesFound) {
            result.eyes = EyeType.NORMAL_2X1;
            result.eyeRow = 4;
        }
        for (int side = 0; side < 2; side++)
            ExpressionDraft.placeEyes(result.data, skin, result.eyes, result.eyeRow, side, null);
        findBrows(result, face, skin, skinColor);
        return result;
    }

    /** How much a pixel looks like part of an eye: its difference from the skin, or a white's, capped. */
    private static double eyeness(int color, int skinColor) {
        double distance = ExpressionDraft.distance(color, skinColor);
        double white = ExpressionSkinExporter.whiteness(color) > 200 && distance > 30 ? 100 : 0;
        return Math.max(Math.min(distance, CAP), white);
    }

    private static boolean white(int color, int skinColor) {
        return ExpressionSkinExporter.whiteness(color) > 150 && ExpressionDraft.distance(color, skinColor) > 30;
    }

    /** Whether column x continues the same color above top and below bottom: a strand of hair, not an eye. */
    private static boolean strand(int[][] face, int x, int top, int bottom) {
        if (top < 1 || bottom > 6) return false;
        return ExpressionDraft.distance(face[x][top - 1], face[x][top]) < 50
            && ExpressionDraft.distance(face[x][bottom + 1], face[x][bottom]) < 50;
    }

    static double score(int[][] face, int skinColor, EyeType type, int row) {
        double score = 0;
        int bottom = row + type.height - 1;
        for (int side = 0; side < 2; side++) {
            int first = side == 0 ? type.x : 8 - type.x - type.width, last = first + type.width - 1;
            for (int y = row; y <= bottom; y++) {
                double rowScore = 0;
                boolean allSkin = true, anyWhite = false;
                int lightest = 0, darkest = 255;
                for (int x = first; x <= last; x++) {
                    double value = eyeness(face[x][y], skinColor) - THRESHOLD;
                    if (value > 0 && strand(face, x, row, bottom)) value = -value;
                    rowScore += value;
                    allSkin &= eyeness(face[x][y], skinColor) < THRESHOLD;
                    anyWhite |= white(face[x][y], skinColor);
                    int luma = ExpressionDraft.luma(face[x][y]);
                    lightest = Math.max(lightest, luma);
                    darkest = Math.min(darkest, luma);
                }
                boolean eyeRow = anyWhite || lightest - darkest > 90;
                // A dull top row of a taller eye is more likely a brow or a lash line above the eye.
                if (type.height > 1 && y == row && !eyeRow && rowScore > 0) rowScore = -rowScore;
                if (eyeRow && type.width > 1) rowScore += 40;
                if (allSkin) rowScore -= 60;
                score += rowScore;
            }
            // Eye colored pixels beside or below the eye mean it is larger than this shape.
            for (int y = row; y <= bottom; y++) for (int x : new int[] { first - 1, last + 1 }) {
                if (x < 0 || x > 7 || (x < 4) != (side == 0) || strand(face, x, row, bottom)) continue;
                double value = eyeness(face[x][y], skinColor) - THRESHOLD;
                if (value > 0) score -= value * 0.6;
            }
            if (bottom < 7) for (int x = first; x <= last; x++) {
                double value = eyeness(face[x][bottom + 1], skinColor) - THRESHOLD;
                if (value > 0) score -= value * 0.6;
            }
        }
        // Rows whose middle differs from the skin are a mouth's or a beard's, not the eyes'.
        for (int y = row; y <= bottom; y++) for (int x = 3; x <= 4; x++) {
            double value = Math.min(ExpressionDraft.distance(face[x][y], skinColor), CAP) - THRESHOLD;
            if (value > 0) score -= value;
        }
        if (type != EyeType.NORMAL_2X1 && type != EyeType.NORMAL_2X2 && type != EyeType.NORMAL_2X3) score -= 40;
        return score;
    }

    /**
     * Brows: a row above the eyes whose brow pixels differ from the skin while the pixels around them are skin, so
     * neither a hairline nor a mask. Paired brows on rows 2-5, wide-set and joined ones on row 3.
     */
    private static void findBrows(Result result, int[][] face, BufferedImage skin, int skinColor) {
        if (!result.eyesFound) return;
        double best = 0;
        Brows kind = null;
        int bestRow = -1;
        for (int row = 2; row <= Math.min(5, result.eyeRow - 1); row++) {
            for (Brows brows : Brows.values()) {
                if (brows != Brows.PAIRED && row != 3) continue;
                int[] in = brows == Brows.JOINED ? new int[] { 1, 2, 3, 4, 5, 6 }
                    : brows == Brows.WIDE_SET ? new int[] { 0, 1, 6, 7 } : new int[] { 1, 2, 5, 6 };
                int[] around = brows == Brows.JOINED ? new int[] { 0, 7 }
                    : brows == Brows.WIDE_SET ? new int[] { 2, 5 } : new int[] { 3, 4 };
                double sum = 0;
                boolean fits = true, hairline = true;
                for (int x : in) {
                    double distance = ExpressionDraft.distance(face[x][row], skinColor);
                    fits &= distance > ExpressionDraft.SKIN_NEAR;
                    sum += distance;
                    hairline &= ExpressionDraft.distance(face[x][row - 1], face[x][row]) < 30;
                }
                for (int x : around)
                    fits &= ExpressionDraft.distance(face[x][row], skinColor) < ExpressionDraft.SKIN_NEAR;
                if (!fits || hairline) continue;
                // Closer to the eyes wins over a farther row as strong.
                double score = sum / in.length + row * 2;
                if (score > best) {
                    best = score;
                    kind = brows;
                    bestRow = row;
                }
            }
        }
        if (kind == null) return;
        result.brows = kind;
        result.browRow = bestRow;
        ExpressionDraft.placeBrows(result.data, skin, kind, bestRow, -1);
    }
}
