package org.fentanylsolutions.salamander.compat.wawelauth;

import org.fentanylsolutions.salamander.cem.model.CemModel;

/**
 * Recognizes CEM boxes that are a whole overlay piece of the vanilla 64x64 player skin: the vanilla UV layout, size
 * and inflation, wherever the pack places and animates them. Wawel Auth can draw those itself, flat or 3D. Holds no
 * Wawel Auth types.
 */
public final class WawelAuthOverlays {

    private static final float EPSILON = 1.0e-3f;

    /** The vanilla overlay pieces: part name, UV origin, size and inflation. Sleeves come classic and slim. */
    private static final Net[] NETS = { new Net("HAT", 32, 0, 8, 8, 8, 0.5f),
        new Net("JACKET", 16, 32, 8, 12, 4, 0.25f), new Net("RIGHT_SLEEVE", 40, 32, 4, 12, 4, 0.25f),
        new Net("RIGHT_SLEEVE", 40, 32, 3, 12, 4, 0.25f), new Net("LEFT_SLEEVE", 48, 48, 4, 12, 4, 0.25f),
        new Net("LEFT_SLEEVE", 48, 48, 3, 12, 4, 0.25f), new Net("RIGHT_PANTS", 0, 32, 4, 12, 4, 0.25f),
        new Net("LEFT_PANTS", 0, 48, 4, 12, 4, 0.25f) };

    private WawelAuthOverlays() {}

    /** The overlay piece this box draws, or null when it is anything else. */
    public static Overlay match(CemModel.Box box) {
        if (box.textureWidth != 64 || box.textureHeight != 64 || box.mirrorU || box.mirrorV) return null;
        for (Net net : NETS) {
            if (!sized(box, net) || !mapped(box, net)) continue;
            // The vanilla box's origin is its inflated corner moved in by the vanilla inflation.
            float[] c = box.coordinates;
            return new Overlay(
                net.part,
                c[0] - box.inflation[0] + net.inflation,
                c[1] - box.inflation[1] + net.inflation,
                c[2] - box.inflation[2] + net.inflation,
                net.width,
                net.height,
                net.depth,
                net.u,
                net.v);
        }
        return null;
    }

    /** The drawn size is the vanilla size plus the vanilla inflation, however the pack spells it. */
    private static boolean sized(CemModel.Box box, Net net) {
        int[] size = { net.width, net.height, net.depth };
        for (int axis = 0; axis < 3; axis++) {
            float drawn = box.coordinates[axis + 3] + 2 * box.inflation[axis];
            if (Math.abs(drawn - (size[axis] + 2 * net.inflation)) > EPSILON) return false;
        }
        return true;
    }

    /**
     * Box UVs must be the vanilla box exactly. Per-face UVs must give the same picture as the vanilla box: the same
     * side rectangles, and top and bottom rotated half a turn, as per-face top and bottom rectangles start at the
     * opposite corner.
     */
    private static boolean mapped(CemModel.Box box, Net net) {
        if (box.faceUvs == null) {
            return box.uv[0] == net.u && box.uv[1] == net.v
                && box.coordinates[3] == net.width
                && box.coordinates[4] == net.height
                && box.coordinates[5] == net.depth;
        }
        float[][] expected = net.faces();
        for (int face = 0; face < 6; face++) {
            float[] actual = box.faceUvs[face];
            if (actual == null) return false;
            float[] wanted = expected[face];
            if (face == 2 || face == 3) wanted = new float[] { wanted[2], wanted[3], wanted[0], wanted[1] };
            for (int i = 0; i < 4; i++) if (Math.abs(actual[i] - wanted[i]) > EPSILON) return false;
        }
        return true;
    }

    /** A matched box as a vanilla overlay piece, in the box's own frame. */
    public static final class Overlay {

        public final String part;
        public final float x, y, z;
        public final int width, height, depth, u, v;
        /** The Wawel Auth segment, made on first use. */
        Object segment;

        Overlay(String part, float x, float y, float z, int width, int height, int depth, int u, int v) {
            this.part = part;
            this.x = x;
            this.y = y;
            this.z = z;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.u = u;
            this.v = v;
        }
    }

    private static final class Net {

        final String part;
        final int u, v, width, height, depth;
        final float inflation;

        Net(String part, int u, int v, int width, int height, int depth, float inflation) {
            this.part = part;
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.inflation = inflation;
        }

        /** Box-UV rectangles in CemBoxMesh's face order: east, west, down, up, north, south. */
        float[][] faces() {
            float w = width, h = height, d = depth;
            return new float[][] { { u + d + w, v + d, u + d + w + d, v + d + h }, { u, v + d, u + d, v + d + h },
                { u + d, v, u + d + w, v + d }, { u + d + w, v + d, u + d + w + w, v },
                { u + d, v + d, u + d + w, v + d + h }, { u + d + w + d, v + d, u + d + w + d + w, v + d + h } };
        }
    }
}
