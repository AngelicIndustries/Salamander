package industries.angelic.salamander.skin.expression;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import industries.angelic.salamander.skin.expression.ExpressionDraft.Brows;
import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EyeType;

/**
 * The expression editor's face, drawn as Wawel Auth draws the head of its skin buttons' player: a thin light grey
 * outline, a darker grey face, and two light square eyes, each two texels square and a texel in from the outline.
 * While hovered the eyes move as Just Expressions' do: a blink, then a glance left and right, both eyes shifting a
 * texel; otherwise they look ahead. Each button keeps its own icon, so hovering one starts its animation from the
 * blink. The editor's shape thumbnails are drawn the same way.
 */
public final class FaceIcon {

    /** Wawel Auth's head colors, before the button's tint. */
    static final int OUTLINE = 0xFFE7E7E7, FILL = 0xFF9C9CA1;
    /** The thumbnails' pupils: darker than the face, so each eye shows its whole size. */
    private static final int PUPIL = 0xFF4A4A4F;

    /** What the eyes do. */
    enum Eyes {
        AHEAD,
        LEFT,
        RIGHT,
        CLOSED
    }

    private static final long BLINK_MS = 150L;
    private static final long GLANCE_MS = 600L;
    private static final long CYCLE_MS = 2800L;

    private long hoveredSince = -1L;

    /**
     * Draws the face filling the box x, y, width, height (GUI pixels) as far as whole screen pixels per texel allow,
     * centered, tinted (ARGB, multiplied with the head's colors), leaving texturing and blending as they were.
     */
    public void draw(float x, float y, float width, float height, int tint, boolean hovered) {
        long now = Minecraft.getSystemTime();
        if (!hovered) this.hoveredSince = -1L;
        else if (this.hoveredSince < 0L) this.hoveredSince = now;
        Eyes eyes = eyes(hovered ? now - this.hoveredSince : -1L);
        float[] fit = fit(x, y, width, height, guiScale());
        float left = fit[0], top = fit[1], pixel = fit[2], outline = fit[3];
        begin();
        square(left, top, pixel, outline, tint);
        int light = tinted(OUTLINE, tint);
        for (int column : eyeColumns(eyes)) {
            if (eyes == Eyes.CLOSED) {
                // Closed: a thin line across the middle of each eye.
                rect(
                    left + column * pixel,
                    top + 4 * pixel - outline / 2,
                    left + (column + 2) * pixel,
                    top + 4 * pixel + outline / 2,
                    light);
            } else {
                rect(left + column * pixel, top + 3 * pixel, left + (column + 2) * pixel, top + 5 * pixel, light);
            }
        }
        end();
    }

    /** The first texel column of each eye: one in from the outline, shifted a texel to glance. */
    static int[] eyeColumns(Eyes eyes) {
        int shift = eyes == Eyes.LEFT ? -1 : eyes == Eyes.RIGHT ? 1 : 0;
        return new int[] { 1 + shift, 5 + shift };
    }

    /**
     * Where the face goes in a box: left, top, texel size and outline thickness, in GUI pixels. The texel is the
     * largest whole number of screen pixels that fits eight times; the outline half a texel, at least a screen pixel.
     */
    static float[] fit(float x, float y, float width, float height, int scale) {
        int screenTexel = Math.max(1, (int) Math.floor(Math.min(width, height) * scale / 8));
        float pixel = (float) screenTexel / scale;
        float outline = (float) Math.max(1, screenTexel / 2) / scale;
        return new float[] { x + (width - 8 * pixel) / 2, y + (height - 8 * pixel) / 2, pixel, outline };
    }

    private static int guiScale() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight).getScaleFactor();
    }

    /**
     * An eye shape's thumbnail: the head with this shape's eyes, light, the pupil (where the shape has one) a dark
     * texel at the format's place, darker than the face.
     */
    static void drawShape(float x, float y, float pixel, int tint, EyeType type) {
        begin();
        square(x, y, pixel, 1, tint);
        int light = tinted(OUTLINE, tint), pupilColor = tinted(PUPIL, tint);
        for (int side = 0; side < 2; side++) {
            int first = side == 0 ? type.x : 8 - type.x - type.width;
            int pupil = type.width == 1 ? -1 : first + (type.width == 3 ? 1 : side == 0 ? 1 : 0);
            for (int dx = 0; dx < type.width; dx++) for (int dy = 0; dy < type.height; dy++) {
                int cx = first + dx, cy = 3 + dy;
                texel(x, y, pixel, cx, cy, cx == pupil && dy < 2 ? pupilColor : light);
            }
        }
        end();
    }

    /** A brow layout's thumbnail: the head with light brows on row 3 over 2x1 eyes on row 5. */
    static void drawBrows(float x, float y, float pixel, int tint, Brows kind) {
        begin();
        square(x, y, pixel, 1, tint);
        int light = tinted(OUTLINE, tint);
        for (int cx = 0; cx < 8; cx++) {
            boolean brow = kind == Brows.JOINED ? cx >= 1 && cx <= 6
                : kind == Brows.WIDE_SET ? cx < 2 || cx > 5 : cx == 1 || cx == 2 || cx == 5 || cx == 6;
            if (brow) texel(x, y, pixel, cx, 3, light);
            if (cx == 1 || cx == 6) texel(x, y, pixel, cx, 5, light);
        }
        end();
    }

    private static void square(float x, float y, float pixel, float outline, int tint) {
        float size = 8 * pixel;
        rect(x, y, x + size, y + size, tinted(OUTLINE, tint));
        rect(x + outline, y + outline, x + size - outline, y + size - outline, tinted(FILL, tint));
    }

    /** A texel, kept inside the one pixel outline where it touches it. */
    private static void texel(float x, float y, float pixel, int cx, int cy, int color) {
        float size = 8 * pixel;
        float left = Math.max(x + 1, x + cx * pixel), right = Math.min(x + size - 1, x + (cx + 1) * pixel);
        float top = Math.max(y + 1, y + cy * pixel), bottom = Math.min(y + size - 1, y + (cy + 1) * pixel);
        rect(left, top, right, bottom, color);
    }

    /** color multiplied by tint, channel by channel, alpha included. */
    static int tinted(int color, int tint) {
        int a = (color >>> 24) * (tint >>> 24) / 255;
        int r = (color >> 16 & 255) * (tint >> 16 & 255) / 255;
        int g = (color >> 8 & 255) * (tint >> 8 & 255) / 255;
        int b = (color & 255) * (tint & 255) / 255;
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static boolean texture, blend;

    private static void begin() {
        texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        Tessellator.instance.startDrawingQuads();
    }

    private static void end() {
        Tessellator.instance.draw();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        if (texture) GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (!blend) GL11.glDisable(GL11.GL_BLEND);
    }

    private static void rect(float left, float top, float right, float bottom, int color) {
        if (right <= left || bottom <= top) return;
        Tessellator tessellator = Tessellator.instance;
        tessellator.setColorRGBA_I(color & 0xFFFFFF, color >>> 24);
        tessellator.addVertex(left, bottom, 0);
        tessellator.addVertex(right, bottom, 0);
        tessellator.addVertex(right, top, 0);
        tessellator.addVertex(left, top, 0);
    }

    /** What the eyes do after hovering this long; looking ahead for a negative time. */
    static Eyes eyes(long hoveredFor) {
        if (hoveredFor < 0L) return Eyes.AHEAD;
        long time = hoveredFor % CYCLE_MS;
        if (time < BLINK_MS) return Eyes.CLOSED;
        if (time < BLINK_MS + GLANCE_MS) return Eyes.AHEAD;
        if (time < BLINK_MS + 2 * GLANCE_MS) return Eyes.LEFT;
        if (time < BLINK_MS + 3 * GLANCE_MS) return Eyes.RIGHT;
        return Eyes.AHEAD;
    }
}
