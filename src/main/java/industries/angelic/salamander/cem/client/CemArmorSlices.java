package industries.angelic.salamander.cem.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;

import industries.angelic.salamander.compat.wawelauth.WawelAuthOverlays;
import industries.angelic.salamander.mixins.early.minecraft.client.AccessorCemModelBox;

/**
 * Vanilla armor on a pack that splits a limb at the elbow or knee, split the way Even More Bends splits its own: each
 * armor box is cut at the joint into closed boxes, inflated all round and capped, and each rides the bone of the slice
 * it covers. A boot below the knee goes with the shin as a whole. The pack's overlay slices say where it splits.
 */
final class CemArmorSlices {

    private static final float EPSILON = 1.0e-3f;
    /** CEM part names to the overlay piece that shares the part's box. */
    private static final Map<String, String> PIECES = new HashMap<>();
    private static final Map<ModelRenderer, Map<WawelAuthOverlays.Overlay, List<TexturedQuad>>> SHARES = new WeakHashMap<>();

    static {
        PIECES.put("head", "HAT");
        PIECES.put("body", "JACKET");
        PIECES.put("right_arm", "RIGHT_SLEEVE");
        PIECES.put("left_arm", "LEFT_SLEEVE");
        PIECES.put("right_leg", "RIGHT_PANTS");
        PIECES.put("left_leg", "LEFT_PANTS");
    }

    private CemArmorSlices() {}

    /** The overlay piece whose slices the armor on this part follows; null for parts without one. */
    static String piece(String part) {
        return PIECES.get(part);
    }

    /** Where the vanilla piece's box starts, relative to its part's pivot. Slim sleeves are a pixel narrower. */
    static float[] vanillaOrigin(WawelAuthOverlays.Overlay slice) {
        switch (slice.part) {
            case "HAT":
                return new float[] { -4, -8, -4 };
            case "JACKET":
                return new float[] { -4, 0, -2 };
            case "RIGHT_SLEEVE":
                return new float[] { slice.width == 3 ? -2 : -3, -2, -2 };
            case "LEFT_SLEEVE":
                return new float[] { -1, -2, -2 };
            default:
                return new float[] { -2, 0, -2 };
        }
    }

    /** From the part's pivot to the slice bone's frame, in pixels: where the slice puts the vanilla piece. */
    static float[] offset(WawelAuthOverlays.Overlay slice) {
        float[] origin = vanillaOrigin(slice);
        return new float[] { slice.x - origin[0], slice.y - origin[1], slice.z - origin[2] };
    }

    /**
     * The faces of the armor part that ride slice {@code index} of the limb's slices, top to bottom. The joints sit
     * between consecutive slices' rows, in pixels down from the part's pivot.
     */
    static List<TexturedQuad> share(ModelRenderer part, List<WawelAuthOverlays.Overlay> slices, int index) {
        return SHARES.computeIfAbsent(part, ignored -> new IdentityHashMap<>())
            .computeIfAbsent(slices.get(index), ignored -> {
                float top = index == 0 ? Float.NEGATIVE_INFINITY : joint(slices.get(index - 1), slices.get(index));
                float bottom = index == slices.size() - 1 ? Float.POSITIVE_INFINITY
                    : joint(slices.get(index), slices.get(index + 1));
                List<TexturedQuad> faces = new ArrayList<>();
                for (ModelBox box : part.cubeList) {
                    List<TexturedQuad> quads = new ArrayList<>();
                    Collections.addAll(quads, ((AccessorCemModelBox) box).salamander$quads());
                    faces.addAll(split(quads, box.posY1, box.posY2, top, bottom));
                }
                return faces;
            });
    }

    /** Where two consecutive slices meet, between their rows, in pixels down from the part's pivot. */
    static float joint(WawelAuthOverlays.Overlay upper, WawelAuthOverlays.Overlay lower) {
        return vanillaOrigin(upper)[1] + (upper.rowEnd + lower.rowStart) / 2f;
    }

    /**
     * The part of one box between {@code from} and {@code to}, as a box of its own: the box's inflation all round and
     * its rows there stretched over that height. A real end keeps its cap. Of the cut ends only the bottom one gets a
     * cap, so the upper share looks solid where the joint folds away, and it shows the row the cut runs along. The
     * lower share's cut top stays inside the upper share, or shows the box's inside through an open top, as the uncut
     * box would. {@code top} and {@code bottom} are the box's own uninflated ends. A box wholly inside comes back as it
     * is, wholly outside empty.
     */
    static List<TexturedQuad> split(List<TexturedQuad> faces, float top, float bottom, float from, float to) {
        float start = Math.max(top, from), end = Math.min(bottom, to);
        if (end - start < EPSILON) return Collections.emptyList();
        if (start - top < EPSILON && bottom - end < EPSILON) return faces;
        double inflatedTop = Double.POSITIVE_INFINITY;
        for (TexturedQuad face : faces) for (PositionTextureVertex vertex : face.vertexPositions)
            inflatedTop = Math.min(inflatedTop, vertex.vector3D.yCoord);
        double inflation = top - inflatedTop;
        float newTop = (float) (start - inflation), newBottom = (float) (end + inflation);
        double first = (start - top) / (bottom - top), last = (end - top) / (bottom - top);
        List<TexturedQuad> share = new ArrayList<>();
        for (TexturedQuad face : faces) {
            PositionTextureVertex[] vertices = face.vertexPositions;
            boolean flat = true;
            for (PositionTextureVertex vertex : vertices)
                flat &= Math.abs(vertex.vector3D.yCoord - vertices[0].vector3D.yCoord) < EPSILON;
            PositionTextureVertex[] moved = new PositionTextureVertex[vertices.length];
            if (flat) {
                boolean isTop = Math.abs(vertices[0].vector3D.yCoord - inflatedTop) < EPSILON;
                boolean cut = isTop ? start - top >= EPSILON : bottom - end >= EPSILON;
                if (cut && isTop) continue;
                PositionTextureVertex[] front = cut ? front(faces) : null;
                if (cut && front == null) continue;
                for (int i = 0; i < vertices.length; i++) {
                    moved[i] = at(vertices[i], isTop ? newTop : newBottom, null);
                    if (cut) moved[i] = row(moved[i], front, inflatedTop, (end - top - 0.5) / (bottom - top));
                }
            } else {
                float v0 = Float.NaN, v1 = Float.NaN;
                for (PositionTextureVertex vertex : vertices) {
                    if (Math.abs(vertex.vector3D.yCoord - inflatedTop) < EPSILON) v0 = vertex.texturePositionY;
                    else v1 = vertex.texturePositionY;
                }
                for (int i = 0; i < vertices.length; i++) {
                    boolean upper = Math.abs(vertices[i].vector3D.yCoord - inflatedTop) < EPSILON;
                    moved[i] = at(
                        vertices[i],
                        upper ? newTop : newBottom,
                        (float) (v0 + (v1 - v0) * (upper ? first : last)));
                }
            }
            share.add(new TexturedQuad(moved));
        }
        return share;
    }

    /** The box's front side, the one facing -z: the face a cut cap takes its row from. */
    private static PositionTextureVertex[] front(List<TexturedQuad> faces) {
        double near = Double.POSITIVE_INFINITY;
        for (TexturedQuad face : faces)
            for (PositionTextureVertex vertex : face.vertexPositions) near = Math.min(near, vertex.vector3D.zCoord);
        for (TexturedQuad face : faces) {
            boolean front = true;
            for (PositionTextureVertex vertex : face.vertexPositions)
                front &= Math.abs(vertex.vector3D.zCoord - near) < EPSILON;
            if (front) return face.vertexPositions;
        }
        return null;
    }

    /**
     * A cap corner textured with the front side's texel row at {@code fraction} of the box's height, the column above
     * that corner: the cap shows the row its cut runs along.
     */
    private static PositionTextureVertex row(PositionTextureVertex corner, PositionTextureVertex[] front,
        double inflatedTop, double fraction) {
        float u = corner.texturePositionX, v0 = Float.NaN, v1 = Float.NaN;
        for (PositionTextureVertex vertex : front) {
            if (Math.abs(vertex.vector3D.xCoord - corner.vector3D.xCoord) < EPSILON) u = vertex.texturePositionX;
            if (Math.abs(vertex.vector3D.yCoord - inflatedTop) < EPSILON) v0 = vertex.texturePositionY;
            else v1 = vertex.texturePositionY;
        }
        return new PositionTextureVertex(
            (float) corner.vector3D.xCoord,
            (float) corner.vector3D.yCoord,
            (float) corner.vector3D.zCoord,
            u,
            (float) (v0 + (v1 - v0) * fraction));
    }

    /** The vertex moved to height {@code y}, with texture row {@code v}, or its own when null. */
    private static PositionTextureVertex at(PositionTextureVertex vertex, float y, Float v) {
        return new PositionTextureVertex(
            (float) vertex.vector3D.xCoord,
            y,
            (float) vertex.vector3D.zCoord,
            vertex.texturePositionX,
            v == null ? vertex.texturePositionY : v);
    }
}
