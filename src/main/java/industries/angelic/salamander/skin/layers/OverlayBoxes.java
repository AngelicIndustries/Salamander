package industries.angelic.salamander.skin.layers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.api.skin.LayerSegment;
import industries.angelic.salamander.api.skin.SkinPart;

/**
 * Flat overlay boxes. Their geometry does not depend on the skin, so each segment is compiled once into a display
 * list and shared by every player. Boxes without the texels worn things cover are compiled per coverage mask, also
 * shared, and freed once that mask has not been drawn for a while.
 */
@SideOnly(Side.CLIENT)
final class OverlayBoxes {

    /** Vanilla model scale; lists are compiled at it. */
    static final float MODEL_SCALE = 0.0625F;

    private static final long EVICT_AFTER_MS = 30_000L;

    private static final Map<LayerSegment, Integer> LISTS = new HashMap<>();
    private static final Map<Coverage.Mask, Masked> MASKED = new HashMap<>();

    private OverlayBoxes() {}

    /** Vanilla's 64x64 player model inflates the headwear by 0.5 and the other overlays by 0.25. */
    static float inflate(SkinPart part) {
        return part == SkinPart.HAT ? 0.5F : 0.25F;
    }

    static void draw(LayerSegment segment, float scale) {
        draw(segment, scale, Coverage.Mask.NONE);
    }

    /** Without the texels {@code covered}. */
    static void draw(LayerSegment segment, float scale, Coverage.Mask covered) {
        Integer list;
        if (covered.none()) {
            list = LISTS.get(segment);
            if (list == null) {
                list = compile(segment, covered);
                LISTS.put(segment, list);
            }
        } else {
            Masked masked = MASKED.get(covered);
            if (masked == null) {
                masked = new Masked();
                MASKED.put(covered, masked);
            }
            masked.lastUsed = System.currentTimeMillis();
            list = masked.lists.get(segment);
            if (list == null) {
                list = compile(segment, covered);
                masked.lists.put(segment, list);
            }
        }
        if (scale == MODEL_SCALE) {
            GL11.glCallList(list);
            return;
        }
        float factor = scale / MODEL_SCALE;
        GL11.glPushMatrix();
        GL11.glScalef(factor, factor, factor);
        GL11.glCallList(list);
        GL11.glPopMatrix();
    }

    static void sweep(long now) {
        Iterator<Masked> iterator = MASKED.values()
            .iterator();
        while (iterator.hasNext()) {
            Masked masked = iterator.next();
            if (now - masked.lastUsed > EVICT_AFTER_MS) {
                masked.cleanup();
                iterator.remove();
            }
        }
    }

    static void clearMasked() {
        for (Masked masked : MASKED.values()) masked.cleanup();
        MASKED.clear();
    }

    private static int compile(LayerSegment segment, Coverage.Mask covered) {
        int list = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        paint(segment, covered);
        GL11.glEndList();
        return list;
    }

    private static void paint(LayerSegment segment, Coverage.Mask covered) {
        Tessellator tessellator = Tessellator.instance;
        for (OverlayBoxGeometry.Quad quad : OverlayBoxGeometry.quads(segment, inflate(segment.part()))) {
            tessellator.startDrawingQuads();
            tessellator.setNormal(quad.normalX, quad.normalY, quad.normalZ);
            for (List<float[]> polygon : polygons(quad, covered)) {
                // Keep quads for the legacy tessellator and shader integrations. Larger polygons use a fan;
                // its triangles repeat the last vertex to stay in the same GL_QUADS draw.
                if (polygon.size() == 4) {
                    for (float[] vertex : polygon) vertex(tessellator, vertex);
                } else {
                    for (int i = 1; i + 1 < polygon.size(); i++) {
                        vertex(tessellator, polygon.get(0));
                        vertex(tessellator, polygon.get(i));
                        vertex(tessellator, polygon.get(i + 1));
                        vertex(tessellator, polygon.get(i + 1));
                    }
                }
            }
            tessellator.draw();
        }
    }

    static List<List<float[]>> polygons(OverlayBoxGeometry.Quad quad, Coverage.Mask covered) {
        List<List<float[]>> polygons = new ArrayList<>();
        List<float[]> rectangles = covered.none() ? java.util.Collections.singletonList(new float[] { 0, 1, 0, 1 })
            : open(quad, covered);
        for (float[] r : rectangles) {
            float[][] corners = { { r[1], r[2] }, { r[0], r[2] }, { r[0], r[3] }, { r[1], r[3] } };
            List<float[]> polygon = new ArrayList<>();
            for (float[] corner : corners) polygon.add(point(quad, corner[0], corner[1]));
            polygons.add(polygon);
        }
        return polygons;
    }

    private static void vertex(Tessellator tessellator, float[] vertex) {
        tessellator.addVertexWithUV(
            vertex[0] * MODEL_SCALE,
            vertex[1] * MODEL_SCALE,
            vertex[2] * MODEL_SCALE,
            vertex[3],
            vertex[4]);
    }

    /**
     * The texels of a face that {@code covered} leaves open, as few rectangles as rows allow: s0, s1 along the face's
     * u and t0, t1 along its v, 0 to 1.
     */
    static List<float[]> open(OverlayBoxGeometry.Quad quad, Coverage.Mask covered) {
        List<float[]> rectangles = new ArrayList<>();
        // Vertex 1 sits at (u1, v1), 0 at (u2, v1), 2 at (u1, v2).
        int u1 = Math.round(quad.uvs[2] * OverlayBoxGeometry.TEXTURE_SIZE);
        int v1 = Math.round(quad.uvs[3] * OverlayBoxGeometry.TEXTURE_SIZE);
        int u2 = Math.round(quad.uvs[0] * OverlayBoxGeometry.TEXTURE_SIZE);
        int v2 = Math.round(quad.uvs[5] * OverlayBoxGeometry.TEXTURE_SIZE);
        int columns = Math.abs(u2 - u1);
        int rows = Math.abs(v2 - v1);
        if (columns == 0 || rows == 0) return rectangles;
        boolean[] open = new boolean[columns * rows];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int u = u2 > u1 ? u1 + column : u1 - 1 - column;
                int v = v2 > v1 ? v1 + row : v1 - 1 - row;
                open[row * columns + column] = !covered.covers(u, v);
            }
        }
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (!open[row * columns + column]) continue;
                int right = column + 1;
                while (right < columns && open[row * columns + right]) right++;
                int bottom = row + 1;
                while (bottom < rows && all(open, columns, bottom, column, right)) bottom++;
                for (int r = row; r < bottom; r++) {
                    for (int c = column; c < right; c++) open[r * columns + c] = false;
                }
                rectangles.add(
                    new float[] { column / (float) columns, right / (float) columns, row / (float) rows,
                        bottom / (float) rows });
            }
        }
        return rectangles;
    }

    private static boolean all(boolean[] open, int columns, int row, int from, int to) {
        for (int column = from; column < to; column++) {
            if (!open[row * columns + column]) return false;
        }
        return true;
    }

    private static float[] point(OverlayBoxGeometry.Quad quad, float s, float t) {
        float[] p = quad.positions, uv = quad.uvs;
        return new float[] { p[3] + s * (p[0] - p[3]) + t * (p[6] - p[3]), p[4] + s * (p[1] - p[4]) + t * (p[7] - p[4]),
            p[5] + s * (p[2] - p[5]) + t * (p[8] - p[5]), uv[2] + s * (uv[0] - uv[2]), uv[3] + t * (uv[5] - uv[3]) };
    }

    /** The lists compiled under one coverage mask. */
    private static final class Masked {

        final Map<LayerSegment, Integer> lists = new HashMap<>();
        long lastUsed;

        void cleanup() {
            for (int list : lists.values()) GLAllocation.deleteDisplayLists(list);
            lists.clear();
        }
    }
}
