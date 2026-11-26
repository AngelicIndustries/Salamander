package industries.angelic.salamander.skin.expression;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import industries.angelic.salamander.cem.client.CemSkinImage;
import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EditorData;
import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EyeType;

/**
 * What the editor does to a draft, without its screen: placing eyes, brows, outer brows and eyelashes, colors by
 * feature, pupils picked by hand, and what the packs can't show in a draft, with a fix for most of it. Sides are the
 * two halves of the face as the skin has them: 0 is x 0-3, 1 is x 4-7.
 */
final class ExpressionDraft {

    /** The eye shapes the editor offers. Outer pupils are not a shape here: 2x1 eyes take them from the skin. */
    static final EyeType[] SHAPES = { EyeType.NORMAL_2X1, EyeType.NORMAL_2X2, EyeType.NORMAL_2X3, EyeType.ENDERMAN,
        EyeType.WIDE, EyeType.SIMPLE_1X1, EyeType.SIMPLE_WIDE, EyeType.SIMPLE_1X2, EyeType.SIMPLE_1X3 };

    enum Brows {
        PAIRED,
        WIDE_SET,
        JOINED
    }

    /** A color of one feature, set for all its pixels on a side at once. */
    enum Swatch {
        WHITE,
        PUPIL,
        LID,
        BROW,
        UNDER_BROW,
        OUTER,
        LASH_TOP,
        LASH_SIDE
    }

    /** What picking a pupil by hand did. */
    enum Pick {
        DONE,
        /** This shape keeps its pupil in one place; only 2x1 eyes have a choice. */
        FIXED_SHAPE,
        /** One pixel wide eyes only blink. */
        NO_PUPIL,
        NOT_AN_EYE
    }

    enum Fix {
        NONE,
        CONVERT_LEGACY,
        MOVE_HAT_TO_FACE,
        ONE_LID_PER_COLUMN,
        ONE_WHITE_PER_ROW,
        ONE_JOINED_BROW_COLOR,
        REMOVE_BROWS,
        REMOVE_OUTER_BROWS,
        REMOVE_LASHES,
        PLACE_EYES_AGAIN
    }

    /** Something the packs can't show, by its lang key under salamander.expression.problem. */
    static final class Problem {

        final String key;
        final Fix fix;
        /** The exporter's own words, for what no key covers. */
        final String detail;

        Problem(String key, Fix fix) {
            this(key, fix, null);
        }

        Problem(String key, Fix fix, String detail) {
            this.key = key;
            this.fix = fix;
            this.detail = detail;
        }
    }

    /** How far a color may be from the skin and still count as skin. */
    static final int SKIN_NEAR = 45;
    private static final int DEFAULT_LASH = 0xff1a1a1a;

    private ExpressionDraft() {}

    // Reading the skin

    /** Whether the editor can read this skin: the packs' layout needs a 64x64 or 64x32 skin. */
    static boolean usable(BufferedImage skin) {
        return skin != null && skin.getWidth() == 64 && (skin.getHeight() == 64 || skin.getHeight() == 32);
    }

    /**
     * The face's skin color: the most common shade between the eyes and above the nose (x 3-4, rows 2-5 of the base
     * layer), which hair, eyes and mouths rarely reach.
     */
    static int skinColor(BufferedImage skin) {
        if (!usable(skin)) return 0xffaaaaaa;
        List<List<Integer>> buckets = new ArrayList<>();
        List<Integer> keys = new ArrayList<>();
        for (int y = 2; y <= 5; y++) for (int x = 3; x <= 4; x++) {
            int color = ExpressionSkinExporter.base(skin, x, y);
            int key = (color >> 16 & 255) / 32 << 16 | (color >> 8 & 255) / 32 << 8 | (color & 255) / 32;
            int index = keys.indexOf(key);
            if (index < 0) {
                keys.add(key);
                buckets.add(new ArrayList<>());
                index = keys.size() - 1;
            }
            buckets.get(index)
                .add(color);
        }
        List<Integer> best = null;
        for (List<Integer> bucket : buckets) {
            if (best == null || bucket.size() > best.size()
                || bucket.size() == best.size() && brightness(bucket) > brightness(best)) best = bucket;
        }
        int r = 0, g = 0, b = 0;
        for (int color : best) {
            r += color >> 16 & 255;
            g += color >> 8 & 255;
            b += color & 255;
        }
        int n = best.size();
        return 0xff000000 | r / n << 16 | g / n << 8 | b / n;
    }

    private static int brightness(List<Integer> colors) {
        int sum = 0;
        for (int color : colors) sum += (color >> 16 & 255) + (color >> 8 & 255) + (color & 255);
        return sum;
    }

    static double distance(int a, int b) {
        int r = (a >> 16 & 255) - (b >> 16 & 255), g = (a >> 8 & 255) - (b >> 8 & 255), bl = (a & 255) - (b & 255);
        return Math.sqrt(r * r + g * g + bl * bl);
    }

    static int luma(int color) {
        return (2126 * (color >> 16 & 255) + 7152 * (color >> 8 & 255) + 722 * (color & 255)) / 10000;
    }

    /** The nearest skin colored pixel above or below x, y on the base layer, or the face's skin color. */
    static int skinNear(BufferedImage skin, int x, int y, int skinColor, boolean below) {
        int[] order = below ? new int[] { y + 1, y - 1 } : new int[] { y - 1, y + 1 };
        for (int yy : order) {
            if (yy < 0 || yy > 7) continue;
            int color = ExpressionSkinExporter.base(skin, x, yy);
            if (distance(color, skinColor) < SKIN_NEAR) return color;
        }
        return skinColor;
    }

    // Eyes

    /** An eye's shape, with outer pupils counted as 2x1; null without a supported eye on that side. */
    static EyeType shape(EditorData data, int side) {
        try {
            EyeType type = ExpressionSkinExporter.eyeType(data, side);
            return type == EyeType.DERP ? EyeType.NORMAL_2X1 : type;
        } catch (IllegalArgumentException unsupported) {
            return null;
        }
    }

    /** Whether a side's eye has its pupil on the outer pixel. */
    static boolean outerPupil(EditorData data, int side) {
        try {
            return ExpressionSkinExporter.eyeType(data, side) == EyeType.DERP;
        } catch (IllegalArgumentException unsupported) {
            return false;
        }
    }

    /** The top row of a side's eye, or -1. */
    static int eyeRow(EditorData data, int side) {
        for (int y = 0; y < 8; y++) for (int x = side * 4; x < side * 4 + 4; x++) if (data.eyes[x][y] != 0) return y;
        return -1;
    }

    static boolean hasEyes(EditorData data, int side) {
        return eyeRow(data, side) >= 0;
    }

    /** Whether a shape can have its pupil on either side: only 2x1 eyes next to the face's edge column can. */
    static boolean pupilSideFree(EyeType type) {
        return type == EyeType.NORMAL_2X1 || type == EyeType.DERP;
    }

    /**
     * Places an eye. A pupil picked by hand (outer true or false) holds where the shape can store it; without one,
     * 2x1 eyes take their pupil side from the skin. Lids take the skin above the eye, or the face's skin color.
     */
    static void placeEyes(EditorData data, BufferedImage skin, EyeType type, int row, int side, Boolean outer) {
        if (type == EyeType.DERP) type = EyeType.NORMAL_2X1;
        boolean fromSkin = outer == null || !pupilSideFree(type);
        EyeType placed = !fromSkin && outer ? EyeType.DERP : type;
        ExpressionSkinExporter.eyePreset(data, skin, placed, row, side, fromSkin);
        lids(data, skin, side);
    }

    /** Lids over each eye column: the skin pixel above the eye, if it is skin colored, else the face's skin color. */
    static void lids(EditorData data, BufferedImage skin, int side) {
        int top = eyeRow(data, side);
        if (top < 0 || !usable(skin)) return;
        int skinColor = skinColor(skin);
        for (int x = side * 4; x < side * 4 + 4; x++) {
            int above = top > 0 ? ExpressionSkinExporter.base(skin, x, top - 1) : skinColor;
            int lid = distance(above, skinColor) < SKIN_NEAR ? above : skinColor;
            for (int y = 0; y < 8; y++) if (data.eyes[x][y] != 0) data.lids[x][y] = lid;
        }
    }

    static void removeEyes(EditorData data, int side) {
        for (int x = side * 4; x < side * 4 + 4; x++) for (int y = 0; y < 8; y++) {
            data.eyes[x][y] = 0;
            data.pupils[x][y] = false;
        }
    }

    /**
     * Makes the eye pixel at x, y the pupil, or the white if it is the pupil, where the shape can store it: in 2x1
     * eyes the pupil then moves to the other pixel. On DONE, outer holds the side's new choice; the caller places the
     * eye again with it.
     */
    static Pick pick(EditorData data, int x, int y, Boolean[] outer) {
        int side = x < 4 ? 0 : 1;
        if (data.eyes[x][y] == 0) return Pick.NOT_AN_EYE;
        EyeType type = shape(data, side);
        if (type == null) return Pick.NOT_AN_EYE;
        if (type.width == 1) return Pick.NO_PUPIL;
        if (!pupilSideFree(type)) return Pick.FIXED_SHAPE;
        boolean clickedOuter = x == (side == 0 ? 1 : 6);
        outer[side] = data.pupils[x][y] ? !clickedOuter : clickedOuter;
        return Pick.DONE;
    }

    // Brows

    /** The brows' layout, or null without brows. */
    static Brows brows(EditorData data) {
        boolean any = false;
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) any |= data.brows[x][y] != 0;
        if (!any) return null;
        boolean joined = true;
        for (int x = 1; x <= 6; x++) joined &= data.brows[x][3] != 0;
        if (joined) return Brows.JOINED;
        return data.brows[0][3] != 0 || data.brows[7][3] != 0 ? Brows.WIDE_SET : Brows.PAIRED;
    }

    /** The row of a side's brow, or -1. */
    static int browRow(EditorData data, int side) {
        for (int y = 0; y < 8; y++) for (int x = side * 4; x < side * 4 + 4; x++) if (data.brows[x][y] != 0) return y;
        return -1;
    }

    /**
     * Places brows of this layout. Paired brows go on rows 2-5 and on one side, or both (side -1); wide-set and joined
     * brows sit on row 3, and joined brows span both sides. Each brow takes its pixel's color, and its cover the skin
     * next to it, which shows where the brow was once it moves.
     */
    static void placeBrows(EditorData data, BufferedImage skin, Brows kind, int row, int side) {
        boolean replaceAll = kind != Brows.PAIRED || side < 0 || brows(data) == Brows.JOINED;
        row = kind == Brows.PAIRED ? Math.max(2, Math.min(5, row)) : 3;
        int skinColor = skinColor(skin);
        for (int x = 0; x < 8; x++) {
            if (!replaceAll && (x < 4 ? 0 : 1) != side) continue;
            for (int y = 0; y < 8; y++) data.brows[x][y] = 0;
            boolean selected = kind == Brows.JOINED ? x >= 1 && x <= 6
                : kind == Brows.WIDE_SET ? x < 2 || x > 5 : x == 1 || x == 2 || x == 5 || x == 6;
            if (!selected) continue;
            data.brows[x][row] = (byte) (kind == Brows.JOINED ? 1 : x < 4 ? 1 : 2);
            data.browColors[x][row] = ExpressionSkinExporter.base(skin, x, row);
            data.browBackgrounds[x][row] = skinNear(skin, x, row, skinColor, false);
        }
        if (kind == Brows.JOINED) {
            data.browColors[4][3] = data.browColors[3][3];
            data.browBackgrounds[4][3] = data.browBackgrounds[3][3];
        }
    }

    /** Removes the moving brows, and the outer brows with them, so no brow is left on a tab out of sight. */
    static void removeBrows(EditorData data) {
        data.clearBrows();
    }

    // Outer brows and eyelashes

    static boolean hasOuterBrows(EditorData data, int side) {
        for (int x = side * 4; x < side * 4 + 4; x++)
            for (int y = 0; y < 8; y++) if (data.overlayBrows[x][y] != 0) return true;
        return false;
    }

    static int outerRow(EditorData data, int side) {
        for (int y = 0; y < 8; y++)
            for (int x = side * 4; x < side * 4 + 4; x++) if (data.overlayBrows[x][y] != 0) return y;
        return -1;
    }

    /** Puts a pair of outer brows on rows 2-5 of a side, keeping their color, or the brow's, or a dark skin shade. */
    static void placeOuterBrows(EditorData data, BufferedImage skin, int row, int side) {
        row = Math.max(2, Math.min(5, row));
        int first = side == 0 ? 1 : 5;
        int color = 0;
        for (int x = first; x < first + 2; x++) for (int y = 0; y < 8; y++) if (data.overlayBrows[x][y] != 0) {
            color = data.overlayColors[x][y];
        }
        if ((color >>> 24) == 0) {
            int brow = browRow(data, side);
            color = brow >= 0 ? data.browColors[first][brow] : darker(skinColor(skin));
        }
        if ((color >>> 24) == 0) color = darker(skinColor(skin));
        removeOuterBrows(data, side);
        for (int x = first; x < first + 2; x++) {
            data.overlayBrows[x][row] = (byte) (side + 1);
            data.overlayColors[x][row] = color;
        }
    }

    static void removeOuterBrows(EditorData data, int side) {
        for (int x = side * 4; x < side * 4 + 4; x++) {
            Arrays.fill(data.overlayBrows[x], (byte) 0);
            Arrays.fill(data.overlayColors[x], 0);
        }
    }

    /** Whether eyelashes fit a side's eye: the packs draw them for 2x2 eyes at the bottom (rows 5-6) only. */
    static boolean lashesFit(EditorData data, int side) {
        return shape(data, side) == EyeType.NORMAL_2X2 && eyeRow(data, side) == 5;
    }

    static boolean hasLashes(EditorData data, int side) {
        for (int x = side * 4; x < side * 4 + 4; x++)
            for (int y = 0; y < 8; y++) if ((data.lashes[x][y] >>> 24) != 0) return true;
        return false;
    }

    /** The pixels a side's eyelashes may use: two above the eye, then two along its outer edge. */
    static int[][] lashPixels(int side, boolean top) {
        int first = side == 0 ? 1 : 5, edge = side == 0 ? 0 : 7;
        return top ? new int[][] { { first, 4 }, { first + 1, 4 } } : new int[][] { { edge, 5 }, { edge, 6 } };
    }

    static void setLashes(EditorData data, int side, boolean on) {
        for (int x = side * 4; x < side * 4 + 4; x++) Arrays.fill(data.lashes[x], 0);
        if (!on) return;
        for (boolean top : new boolean[] { true, false })
            for (int[] pixel : lashPixels(side, top)) data.lashes[pixel[0]][pixel[1]] = DEFAULT_LASH;
    }

    private static int darker(int color) {
        return 0xff000000 | (color >> 16 & 255) * 2 / 5 << 16 | (color >> 8 & 255) * 2 / 5 << 8 | (color & 255) * 2 / 5;
    }

    // Colors by feature

    /** The pixels a swatch colors on a side, as x, y pairs. */
    static List<int[]> pixels(EditorData data, Swatch swatch, int side) {
        List<int[]> pixels = new ArrayList<>();
        EyeType type = shape(data, side);
        for (int x = side * 4; x < side * 4 + 4; x++) for (int y = 0; y < 8; y++) {
            boolean in;
            switch (swatch) {
                case WHITE:
                    in = data.eyes[x][y] != 0 && type != null && type.width > 1;
                    break;
                case PUPIL:
                    in = data.pupils[x][y];
                    break;
                case LID:
                    in = data.eyes[x][y] != 0;
                    break;
                case BROW:
                case UNDER_BROW:
                    in = data.brows[x][y] != 0;
                    break;
                case OUTER:
                    in = data.overlayBrows[x][y] != 0;
                    break;
                default:
                    in = false;
                    break;
            }
            if (in) pixels.add(new int[] { x, y });
        }
        if (swatch == Swatch.LASH_TOP || swatch == Swatch.LASH_SIDE) {
            for (int[] pixel : lashPixels(side, swatch == Swatch.LASH_TOP))
                if ((data.lashes[pixel[0]][pixel[1]] >>> 24) != 0) pixels.add(pixel);
        }
        return pixels;
    }

    private static int[][] layer(EditorData data, Swatch swatch) {
        switch (swatch) {
            case WHITE:
                return data.whites;
            case PUPIL:
                return data.pupilColors;
            case LID:
                return data.lids;
            case BROW:
                return data.browColors;
            case UNDER_BROW:
                return data.browBackgrounds;
            case OUTER:
                return data.overlayColors;
            default:
                return data.lashes;
        }
    }

    /** The different colors a swatch has on a side, at most three, for drawing it. */
    static int[] colors(EditorData data, Swatch swatch, int side) {
        Set<Integer> colors = new LinkedHashSet<>();
        int[][] layer = layer(data, swatch);
        for (int[] pixel : pixels(data, swatch, side)) colors.add(layer[pixel[0]][pixel[1]]);
        return colors.stream()
            .limit(3)
            .mapToInt(Integer::intValue)
            .toArray();
    }

    /** Sets a swatch's color on all its pixels of a side. Joined brows share their middle, so color both sides. */
    static void paint(EditorData data, Swatch swatch, int side, int color) {
        int[][] layer = layer(data, swatch);
        for (int[] pixel : pixels(data, swatch, side)) layer[pixel[0]][pixel[1]] = color;
        if ((swatch == Swatch.BROW || swatch == Swatch.UNDER_BROW) && brows(data) == Brows.JOINED) {
            layer[4][3] = layer[3][3];
        }
    }

    /** Sets one pixel's color in a swatch's layer, as advanced painting does; a lid colors its whole column. */
    static void paintPixel(EditorData data, Swatch swatch, int x, int y, int color) {
        int[][] layer = layer(data, swatch);
        if (swatch == Swatch.LID) {
            for (int yy = 0; yy < 8; yy++) if (data.eyes[x][yy] != 0 && data.eyes[x][y] != 0) layer[x][yy] = color;
            return;
        }
        if ((swatch == Swatch.BROW || swatch == Swatch.UNDER_BROW) && data.brows[x][y] == 0) return;
        layer[x][y] = color;
        if (brows(data) == Brows.JOINED && y == 3 && (x == 3 || x == 4)) layer[7 - x][y] = color;
    }

    // Problems and fixes

    /** What the packs can't show in this draft, most important first; empty when it can be saved. */
    static List<Problem> problems(BufferedImage skin, EditorData data) {
        List<Problem> problems = new ArrayList<>();
        if (skin == null) return problems;
        if (!usable(skin)) {
            problems.add(new Problem("size", Fix.NONE));
            return problems;
        }
        if (skin.getHeight() == 32) problems.add(new Problem("legacy", Fix.CONVERT_LEGACY));
        boolean anyEyes = false, misplaced = false;
        for (int side = 0; side < 2; side++) {
            if (!hasEyes(data, side)) continue;
            anyEyes = true;
            if (shape(data, side) == null) misplaced = true;
        }
        if (!anyEyes) problems.add(new Problem("noEyes", Fix.NONE));
        if (misplaced) problems.add(new Problem("eyePlace", Fix.PLACE_EYES_AGAIN));
        boolean hat = false, lids = false, whites = false;
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) {
            if (data.eyes[x][y] == 0) continue;
            hat |= (skin.getRGB(40 + x, 8 + y) >>> 24) != 0;
            int top = topOfColumn(data, x);
            lids |= opaque(data.lids[x][y]) != opaque(data.lids[x][top]);
        }
        for (int side = 0; side < 2 && !misplaced; side++) {
            EyeType type = shape(data, side);
            if (type == null || type.width != 2) continue;
            int first = side == 0 ? type.x : 8 - type.x - 2;
            for (int y = 0; y < 8; y++) if (data.eyes[first][y] != 0)
                whites |= opaque(data.whites[first][y]) != opaque(data.whites[first + 1][y]);
        }
        if (hat) problems.add(new Problem("hatOverEyes", Fix.MOVE_HAT_TO_FACE));
        if (lids) problems.add(new Problem("lidColumns", Fix.ONE_LID_PER_COLUMN));
        if (whites) problems.add(new Problem("whiteRows", Fix.ONE_WHITE_PER_ROW));
        Brows brows = brows(data);
        if (brows == Brows.JOINED) {
            if (data.browColors[3][3] != data.browColors[4][3]
                || data.browBackgrounds[3][3] != data.browBackgrounds[4][3])
                problems.add(new Problem("joinedBrow", Fix.ONE_JOINED_BROW_COLOR));
        } else if (brows != null && !browsFit(data)) problems.add(new Problem("browPlace", Fix.REMOVE_BROWS));
        if (!outerBrowsFit(data)) problems.add(new Problem("outerPlace", Fix.REMOVE_OUTER_BROWS));
        for (int side = 0; side < 2; side++) if (hasLashes(data, side) && !lashesFit(data, side)) {
            problems.add(new Problem("lashes", Fix.REMOVE_LASHES));
            break;
        }
        if (problems.isEmpty()) {
            try {
                ExpressionSkinExporter.export(skin, data);
            } catch (IllegalArgumentException other) {
                problems.add(new Problem(null, Fix.NONE, other.getMessage()));
            }
        }
        return problems;
    }

    private static int topOfColumn(EditorData data, int x) {
        for (int y = 0; y < 8; y++) if (data.eyes[x][y] != 0) return y;
        return 0;
    }

    private static boolean browsFit(EditorData data) {
        for (int side = 0; side < 2; side++) {
            int count = 0, row = -1;
            for (int x = side * 4; x < side * 4 + 4; x++) for (int y = 0; y < 8; y++) if (data.brows[x][y] != 0) {
                count++;
                row = y;
            }
            if (count == 0) continue;
            int x = side == 0 ? 1 : 5;
            if (row == 3 && data.brows[side == 0 ? 0 : 7][3] != 0) x = side == 0 ? 0 : 6;
            if (count != 2 || row < 2 || row > 5 || data.brows[x][row] == 0 || data.brows[x + 1][row] == 0)
                return false;
        }
        return true;
    }

    private static boolean outerBrowsFit(EditorData data) {
        for (int side = 0; side < 2; side++) {
            int count = 0, row = -1, x = side == 0 ? 1 : 5;
            for (int xx = side * 4; xx < side * 4 + 4; xx++)
                for (int y = 0; y < 8; y++) if (data.overlayBrows[xx][y] != 0) {
                    count++;
                    row = y;
                }
            if (count != 0 && (count != 2 || row < 2
                || row > 5
                || data.overlayBrows[x][row] == 0
                || data.overlayBrows[x + 1][row] == 0)) return false;
        }
        return true;
    }

    /**
     * Applies a fix to the draft. Moving the hat to the face changes the skin itself, so it returns the new skin;
     * every other fix returns the skin as it was. PLACE_EYES_AGAIN is the editor's, which knows the shape to use.
     */
    static BufferedImage fix(Fix fix, BufferedImage skin, EditorData data) {
        switch (fix) {
            case CONVERT_LEGACY:
                return CemSkinImage.normalize(skin);
            case MOVE_HAT_TO_FACE:
                return hatToFace(skin, data);
            case ONE_LID_PER_COLUMN:
                for (int x = 0; x < 8; x++) {
                    int top = topOfColumn(data, x);
                    for (int y = 0; y < 8; y++) if (data.eyes[x][y] != 0) data.lids[x][y] = data.lids[x][top];
                }
                break;
            case ONE_WHITE_PER_ROW:
                for (int side = 0; side < 2; side++) {
                    EyeType type = shape(data, side);
                    if (type == null || type.width != 2) continue;
                    int first = side == 0 ? type.x : 8 - type.x - 2;
                    for (int y = 0; y < 8; y++) if (data.eyes[first][y] != 0) {
                        int a = data.whites[first][y], b = data.whites[first + 1][y];
                        int white = ExpressionSkinExporter.whiteness(a) >= ExpressionSkinExporter.whiteness(b) ? a : b;
                        data.whites[first][y] = data.whites[first + 1][y] = white;
                    }
                }
                break;
            case ONE_JOINED_BROW_COLOR:
                data.browColors[4][3] = data.browColors[3][3];
                data.browBackgrounds[4][3] = data.browBackgrounds[3][3];
                break;
            case REMOVE_BROWS:
                for (int x = 0; x < 8; x++) {
                    Arrays.fill(data.brows[x], (byte) 0);
                    Arrays.fill(data.browColors[x], 0);
                    Arrays.fill(data.browBackgrounds[x], 0);
                }
                break;
            case REMOVE_OUTER_BROWS:
                for (int side = 0; side < 2; side++) removeOuterBrows(data, side);
                break;
            case REMOVE_LASHES:
                for (int side = 0; side < 2; side++) setLashes(data, side, false);
                break;
            default:
                break;
        }
        return skin;
    }

    /**
     * A copy of the skin whose hat pixels over the eyes are drawn onto the face instead: each face pixel takes the
     * color it showed with the hat over it, and the hat there becomes clear. The face looks as it did.
     */
    static BufferedImage hatToFace(BufferedImage skin, EditorData data) {
        BufferedImage result = CemSkinImage.copy(skin);
        if (result == null) return skin;
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) {
            if (data.eyes[x][y] == 0) continue;
            int hat = result.getRGB(40 + x, 8 + y);
            if ((hat >>> 24) == 0) continue;
            result.setRGB(8 + x, 8 + y, over(hat, result.getRGB(8 + x, 8 + y)));
            result.setRGB(40 + x, 8 + y, 0);
        }
        return result;
    }

    /** top drawn over an opaque bottom. */
    static int over(int top, int bottom) {
        int alpha = top >>> 24;
        if (alpha == 0) return 0xff000000 | bottom;
        if (alpha == 255) return top;
        int inverse = 255 - alpha;
        int r = ((top >> 16 & 255) * alpha + (bottom >> 16 & 255) * inverse) / 255;
        int g = ((top >> 8 & 255) * alpha + (bottom >> 8 & 255) * inverse) / 255;
        int b = ((top & 255) * alpha + (bottom & 255) * inverse) / 255;
        return 0xff000000 | r << 16 | g << 8 | b;
    }

    private static int opaque(int color) {
        return 0xff000000 | color & 0xffffff;
    }
}
