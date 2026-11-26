package industries.angelic.salamander.skin.expression;

import java.awt.image.BufferedImage;

import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EditorData;

/**
 * A draft's face as Just Expressions and Fresh Moves move it: pupils shifted inside their eye whites, lids closing
 * from the top, brows lifted over their cover. The editor draws it flat and on its 3D head.
 */
final class ExpressionFace {

    private ExpressionFace() {}

    /**
     * The face's 8x8 colors, [x][y]. lookX and lookY move the pupils by up to a pixel (positive is right and down as
     * the face is drawn), as far as each eye's white leaves room; blink closes the lids from 0 to 1; browLift raises
     * the brows and outer brows by that many pixels. With hat, the hat layer is drawn over the face, as on a flat
     * preview; without it, a 3D head draws its own.
     */
    static int[][] colors(BufferedImage skin, EditorData data, int lookX, int lookY, float blink, int browLift,
        boolean hat) {
        int[][] face = new int[8][8];
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) face[x][y] = ExpressionSkinExporter.base(skin, x, y);
        for (int side = 0; side < 2; side++) eye(face, data, side, lookX, lookY, blink);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) if (data.brows[x][y] != 0) {
            face[x][y] = 0xff000000 | data.browBackgrounds[x][y];
        }
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) if (data.brows[x][y] != 0 && y - browLift >= 0) {
            face[x][y - browLift] = ExpressionDraft.over(data.browColors[x][y], face[x][y - browLift]);
        }
        if (hat && skin != null) for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) {
            face[x][y] = ExpressionDraft.over(skin.getRGB(40 + x, 8 + y), face[x][y]);
        }
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) {
            if (data.overlayBrows[x][y] != 0 && y - browLift >= 0)
                face[x][y - browLift] = ExpressionDraft.over(data.overlayColors[x][y], face[x][y - browLift]);
            face[x][y] = ExpressionDraft.over(data.lashes[x][y], face[x][y]);
        }
        return face;
    }

    private static void eye(int[][] face, EditorData data, int side, int lookX, int lookY, float blink) {
        int left = 8, right = -1, top = 8, bottom = -1;
        boolean pupils = false;
        for (int x = side * 4; x < side * 4 + 4; x++) for (int y = 0; y < 8; y++) if (data.eyes[x][y] != 0) {
            left = Math.min(left, x);
            right = Math.max(right, x);
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
            pupils |= data.pupils[x][y];
        }
        if (right < 0) return;
        if (pupils) {
            for (int x = left; x <= right; x++)
                for (int y = top; y <= bottom; y++) face[x][y] = 0xff000000 | data.whites[x][y];
            int shiftX = room(data, side, lookX, true), shiftY = room(data, side, lookY, false);
            for (int x = left; x <= right; x++) for (int y = top; y <= bottom; y++) if (data.pupils[x][y]) {
                face[x + shiftX][y + shiftY] = 0xff000000 | data.pupilColors[x][y];
            }
        }
        int closed = Math.round((bottom - top + 1) * Math.max(0, Math.min(1, blink)));
        for (int x = left; x <= right; x++)
            for (int y = top; y < top + closed; y++) face[x][y] = 0xff000000 | data.lids[x][y];
    }

    /** How far the pupils may move toward look on one axis without leaving the eye. */
    private static int room(EditorData data, int side, int look, boolean horizontal) {
        int step = Integer.signum(look);
        if (step == 0) return 0;
        for (int x = side * 4; x < side * 4 + 4; x++) for (int y = 0; y < 8; y++) if (data.pupils[x][y]) {
            int nx = horizontal ? x + step : x, ny = horizontal ? y : y + step;
            if (nx < 0 || nx > 7 || ny < 0 || ny > 7 || data.eyes[nx][ny] != side + 1) return 0;
        }
        return step;
    }
}
