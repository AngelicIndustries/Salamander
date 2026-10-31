package industries.angelic.salamander.cem.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import industries.angelic.salamander.api.skin.LayerSegment;
import industries.angelic.salamander.cem.model.CemModel;

/**
 * Recognizes CEM boxes that are an overlay piece of the vanilla 64x64 player skin, or a slice of its rows: the vanilla
 * UV layout, size and inflation, wherever the pack places and animates them. A pack that splits a limb at the elbow or
 * knee draws each slice as its own box. Salamander's skin layers draw those, flat or 3D.
 */
public final class CemOverlays {

    private static final float EPSILON = 1.0e-3f;
    /** Packs shrink overlays a hair to avoid z-fighting, as Fresh Animations' hat (0.49) and jacket (0.24) do. */
    private static final float INFLATION_SLACK = 0.02f;

    /** The vanilla overlay pieces: part name, UV origin, size and inflation. Sleeves come classic and slim. */
    private static final Net[] NETS = { new Net("HAT", 32, 0, 8, 8, 8, 0.5f),
        new Net("JACKET", 16, 32, 8, 12, 4, 0.25f), new Net("RIGHT_SLEEVE", 40, 32, 4, 12, 4, 0.25f),
        new Net("RIGHT_SLEEVE", 40, 32, 3, 12, 4, 0.25f), new Net("LEFT_SLEEVE", 48, 48, 4, 12, 4, 0.25f),
        new Net("LEFT_SLEEVE", 48, 48, 3, 12, 4, 0.25f), new Net("RIGHT_PANTS", 0, 32, 4, 12, 4, 0.25f),
        new Net("LEFT_PANTS", 0, 48, 4, 12, 4, 0.25f) };

    private CemOverlays() {}

    /**
     * The model's overlay boxes and the piece or slice each draws. Slices only count when the model's slices of that
     * piece cover all of its rows, so a shortened limb isn't mistaken for the start of a split one.
     */
    public static Map<CemModel.Box, Overlay> matchAll(CemModel model) {
        Map<CemModel.Box, Overlay> matches = new IdentityHashMap<>();
        Map<Net, List<Overlay>> slices = new HashMap<>();
        for (CemModel.Node node : model.nodes) for (CemModel.Box box : node.boxes) {
            Overlay overlay = match(box);
            if (overlay == null) continue;
            matches.put(box, overlay);
            if (!overlay.whole()) slices.computeIfAbsent(overlay.net, net -> new ArrayList<>())
                .add(overlay);
        }
        for (Map.Entry<Net, List<Overlay>> entry : slices.entrySet()) {
            if (covers(entry.getValue(), entry.getKey().height)) continue;
            matches.values()
                .removeAll(entry.getValue());
        }
        return matches;
    }

    private static boolean covers(List<Overlay> slices, int height) {
        boolean[] rows = new boolean[height];
        for (Overlay slice : slices) for (int row = slice.rowStart; row < slice.rowEnd; row++) rows[row] = true;
        for (boolean row : rows) if (!row) return false;
        return true;
    }

    /** The overlay piece, or slice of one, this box draws on its own; null when it is anything else. */
    static Overlay match(CemModel.Box box) {
        if (box.textureWidth != 64 || box.textureHeight != 64 || box.mirrorU || box.mirrorV) return null;
        for (Net net : NETS) {
            int start = rowStart(box, net);
            if (start < 0) continue;
            int rows = sliceHeight(box, net);
            if (rows < 1 || start + rows > net.height || !sized(box, net, rows) || !mapped(box, net, start, rows))
                continue;
            // The vanilla box's origin is its inflated corner moved in by the vanilla inflation, and a slice starts
            // its first row below that origin.
            float[] c = box.coordinates;
            return new Overlay(
                net,
                c[0] - box.inflation[0] + net.inflation,
                c[1] - box.inflation[1] + net.inflation - start,
                c[2] - box.inflation[2] + net.inflation,
                start,
                start + rows);
        }
        return null;
    }

    /** The first row of the piece this box shows, from where its side faces start; -1 when not on this net. */
    private static int rowStart(CemModel.Box box, Net net) {
        float start = box.faceUvs == null ? box.uv[1] - net.v
            : box.faceUvs[4] == null ? -1 : box.faceUvs[4][1] - net.v - net.depth;
        int row = Math.round(start);
        return Math.abs(start - row) <= EPSILON && row >= 0 ? row : -1;
    }

    /** How many of the piece's rows this box shows. */
    private static int sliceHeight(CemModel.Box box, Net net) {
        float rows = box.faceUvs == null ? box.coordinates[4] : box.faceUvs[4][3] - box.faceUvs[4][1];
        int height = Math.round(rows);
        return Math.abs(rows - height) <= EPSILON ? height : -1;
    }

    /** The drawn size is the vanilla size plus about the vanilla inflation, however the pack spells it. */
    private static boolean sized(CemModel.Box box, Net net, int rows) {
        int[] size = { net.width, rows, net.depth };
        for (int axis = 0; axis < 3; axis++) {
            float drawn = box.coordinates[axis + 3] + 2 * box.inflation[axis];
            if (Math.abs(drawn - (size[axis] + 2 * net.inflation)) > 2 * INFLATION_SLACK + EPSILON) return false;
        }
        return true;
    }

    /**
     * Box UVs must be the vanilla box exactly, moved down the net by the slice's first row. Per-face UVs must give the
     * same picture as the vanilla box: the same side rectangles, cut to the slice's rows, and top and bottom rotated
     * half a turn, as per-face top and bottom rectangles start at the opposite corner. A slice's cut end is inside the
     * limb, so it may show anything there.
     */
    private static boolean mapped(CemModel.Box box, Net net, int start, int rows) {
        if (box.faceUvs == null) {
            return box.uv[0] == net.u && box.coordinates[3] == net.width
                && box.coordinates[4] == rows
                && box.coordinates[5] == net.depth;
        }
        float[][] expected = net.faces();
        for (int face = 0; face < 6; face++) {
            if (face == 2 && start > 0 || face == 3 && start + rows < net.height) continue;
            float[] actual = box.faceUvs[face];
            if (actual == null) return false;
            float[] wanted = expected[face].clone();
            if (face == 2 || face == 3) wanted = new float[] { wanted[2], wanted[3], wanted[0], wanted[1] };
            else {
                wanted[1] += start;
                wanted[3] = wanted[1] + rows;
            }
            for (int i = 0; i < 4; i++) if (Math.abs(actual[i] - wanted[i]) > EPSILON) return false;
        }
        return true;
    }

    /** A matched box as a vanilla overlay piece, or rows of one, in the box's own frame. */
    public static final class Overlay {

        final Net net;
        public final String part;
        public final float x, y, z;
        public final int width, height, depth, u, v;
        /** The rows of the piece this box draws; all of them for a whole piece. */
        public final int rowStart, rowEnd;
        /** The layer segment, made on first use. */
        LayerSegment segment;

        Overlay(Net net, float x, float y, float z, int rowStart, int rowEnd) {
            this.net = net;
            this.part = net.part;
            this.x = x;
            this.y = y;
            this.z = z;
            this.width = net.width;
            this.height = net.height;
            this.depth = net.depth;
            this.u = net.u;
            this.v = net.v;
            this.rowStart = rowStart;
            this.rowEnd = rowEnd;
        }

        public boolean whole() {
            return rowStart == 0 && rowEnd == height;
        }
    }

    static final class Net {

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
