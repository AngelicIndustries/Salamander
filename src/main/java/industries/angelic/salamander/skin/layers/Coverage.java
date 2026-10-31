package industries.angelic.salamander.skin.layers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.api.skin.LayerSegment;
import industries.angelic.salamander.api.skin.SkinPart;

/**
 * What worn armor and accessories hide of one overlay part. Most are drawn on the vanilla model and textured like a
 * legacy skin, box for box: an overlay texel is covered where the worn texture draws at the same place on the matching
 * base box. Left limbs wear the right limb's texture mirrored, as a legacy skin's left limbs do, and a slim arm lacks
 * the outer column of the arm armor is drawn on. Armor on its own model covers its parts whole. Shared: the same
 * part under the same textures is one instance, so what is built under it can be kept by identity.
 */
@SideOnly(Side.CLIENT)
final class Coverage {

    /** Covers nothing. */
    static final Coverage NONE = new Coverage(SkinPart.HAT, Collections.emptyList(), false);
    /** Covers the whole part: for what cannot be seen into. */
    static final Coverage FULL = new Coverage(SkinPart.HAT, Collections.emptyList(), true);

    /** Vanilla draws armor with an alpha test at 0.1. */
    private static final int DRAWN_ALPHA = 25;
    private static final Map<Key, Coverage> SHARED = new HashMap<>();
    private static final Map<ResourceLocation, Opacity> TEXTURES = new HashMap<>();

    private final SkinPart part;
    private final List<Textured> sources;
    private final boolean full;
    private final Map<LayerSegment, Mask> masks = new HashMap<>();

    private Coverage(SkinPart part, List<Textured> sources, boolean full) {
        this.part = part;
        this.sources = sources;
        this.full = full;
    }

    /** What this covers of the segment's box, shared by its flat and voxel meshes. */
    Mask mask(LayerSegment segment) {
        if (full) return Mask.ALL;
        if (sources.isEmpty()) return Mask.NONE;
        Mask mask = masks.get(segment);
        if (mask == null) {
            mask = mask(part, sources, segment.u(), segment.v(), segment.width(), segment.height(), segment.depth());
            masks.put(segment, mask);
        }
        return mask;
    }

    /** Textures are read again after a resource reload. */
    static void clear() {
        SHARED.clear();
        TEXTURES.clear();
    }

    /** Texel coverage for armor and accessories drawn on the legacy base boxes. */
    static Mask mask(SkinPart part, List<Textured> sources, int u, int v, int width, int height, int depth) {
        for (Textured source : sources) {
            if (!source.fits(part, width, height, depth)) return Mask.UNMAPPED;
        }
        int columns = 2 * (width + depth);
        int rows = depth + height;
        boolean[] covered = new boolean[columns * rows];
        int count = 0;
        int total = 0;
        for (Face face : Face.values()) {
            int faceU = face.u(width, depth);
            int faceV = face.v(depth);
            for (int row = 0; row < face.rows(height, depth); row++) {
                for (int column = 0; column < face.columns(width, depth); column++) {
                    total++;
                    if (!covered(sources, part, face, column, row, width)) continue;
                    covered[(faceV + row) * columns + faceU + column] = true;
                    count++;
                }
            }
        }
        if (count == 0) return Mask.NONE;
        if (count == total) return Mask.ALL;
        return new Mask(u, v, columns, rows, covered);
    }

    private static boolean covered(List<Textured> sources, SkinPart part, Face face, int column, int row, int width) {
        for (Textured source : sources) {
            if (source.covers(part, face, column, row, width)) return true;
        }
        return false;
    }

    static boolean left(SkinPart part) {
        return part == SkinPart.LEFT_SLEEVE || part == SkinPart.LEFT_PANTS;
    }

    /** A worn texture's pixels, read once. */
    static Opacity opacity(ResourceLocation texture) {
        Opacity opacity = TEXTURES.get(texture);
        if (opacity == null) {
            opacity = read(texture);
            TEXTURES.put(texture, opacity);
        }
        return opacity;
    }

    /** An unreadable texture draws as the missing texture does, everywhere. */
    private static Opacity read(ResourceLocation texture) {
        BufferedImage image;
        try (InputStream in = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(texture)
            .getInputStream()) {
            image = ImageIO.read(in);
        } catch (IOException | RuntimeException e) {
            image = null;
        }
        return image == null ? Opacity.EVERYWHERE : Opacity.of(image);
    }

    /** Whether a worn texture draws at a texel of the 64x32 layout. */
    interface Texels {

        boolean drawn(int u, int v);
    }

    /** Which pixels of a worn texture draw. */
    static final class Opacity implements Texels {

        static final Opacity EVERYWHERE = new Opacity(1, 1, new boolean[] { true });

        final int width;
        final int height;
        private final boolean[] opaque;

        private Opacity(int width, int height, boolean[] opaque) {
            this.width = width;
            this.height = height;
            this.opaque = opaque;
        }

        static Opacity of(BufferedImage image) {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
            boolean[] opaque = new boolean[argb.length];
            for (int i = 0; i < argb.length; i++) opaque[i] = (argb[i] >>> 24) > DRAWN_ALPHA;
            return new Opacity(width, height, opaque);
        }

        /** At a texel of the 64x32 layout, at any resolution: any of its pixels. */
        @Override
        public boolean drawn(int u, int v) {
            int x0 = u * width / 64;
            int x1 = Math.max(x0 + 1, (u + 1) * width / 64);
            int y0 = v * height / 32;
            int y1 = Math.max(y0 + 1, (v + 1) * height / 32);
            for (int y = Math.max(0, y0); y < y1 && y < height; y++) {
                for (int x = Math.max(0, x0); x < x1 && x < width; x++) {
                    if (opaque[y * width + x]) return true;
                }
            }
            return false;
        }

        /** At {@code u}, {@code v} across the whole texture, 0 to 1. */
        boolean drawnAt(float u, float v) {
            int x = Math.min(width - 1, Math.max(0, (int) Math.floor(u * width)));
            int y = Math.min(height - 1, Math.max(0, (int) Math.floor(v * height)));
            return opaque[y * width + x];
        }
    }

    /** Base boxes of the legacy 64x32 layout, which worn textures are mapped with. */
    enum Box {

        HEAD(0, 0, 8, 8, 8),
        HEADWEAR(32, 0, 8, 8, 8),
        BODY(16, 16, 8, 12, 4),
        ARM(40, 16, 4, 12, 4),
        LEG(0, 16, 4, 12, 4);

        final int u;
        final int v;
        final int width;
        final int height;
        final int depth;

        Box(int u, int v, int width, int height, int depth) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }
    }

    /** A box's faces, where ModelBox lays them out from the box's texture origin. */
    enum Face {

        TOP,
        BOTTOM,
        X_MIN,
        FRONT,
        X_MAX,
        BACK;

        int u(int width, int depth) {
            return switch (this) {
                case X_MIN -> 0;
                case TOP, FRONT -> depth;
                case BOTTOM, X_MAX -> depth + width;
                case BACK -> 2 * depth + width;
            };
        }

        int v(int depth) {
            return this == TOP || this == BOTTOM ? 0 : depth;
        }

        int columns(int width, int depth) {
            return this == X_MIN || this == X_MAX ? depth : width;
        }

        int rows(int height, int depth) {
            return this == TOP || this == BOTTOM ? depth : height;
        }

        /** Whether its columns start at x min. The back's start at x max, the sides' along z. */
        boolean alongX() {
            return this == TOP || this == BOTTOM || this == FRONT;
        }

        /** The face a mirrored box draws in this one's place. */
        Face mirrored() {
            return switch (this) {
                case X_MIN -> X_MAX;
                case X_MAX -> X_MIN;
                default -> this;
            };
        }
    }

    /** A worn texture drawn on one box of the vanilla model. */
    static final class Textured {

        final ResourceLocation texture;
        final Box box;
        final Texels texels;

        Textured(ResourceLocation texture, Box box, Texels texels) {
            this.texture = texture;
            this.box = box;
            this.texels = texels;
        }

        /** A slim arm is one column narrower than the arm armor is drawn on. */
        boolean fits(SkinPart part, int width, int height, int depth) {
            return height == box.height && depth == box.depth
                && (width == box.width || box == Box.ARM && width == box.width - 1);
        }

        boolean covers(SkinPart part, Face face, int column, int row, int width) {
            boolean left = left(part);
            int worn = column;
            // The missing column is the outer one: at x min on the right arm, at x max on the left.
            if (width < box.width && (left ? face == Face.BACK : face.alongX())) worn++;
            Face wornFace = face;
            if (left) {
                wornFace = face.mirrored();
                worn = wornFace.columns(box.width, box.depth) - 1 - worn;
            }
            int u = box.u + wornFace.u(box.width, box.depth) + worn;
            int v = box.v + wornFace.v(box.depth) + row;
            return texels.drawn(u, v);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Textured other && box == other.box && Objects.equals(texture, other.texture);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hashCode(texture) + box.hashCode();
        }
    }

    /** The covered texels of one overlay box, in the skin layout. */
    static final class Mask {

        static final Mask NONE = new Mask(0, 0, 0, 0, null);
        static final Mask ALL = new Mask(0, 0, 0, 0, null);
        /** Covered somewhere, but the box is not one worn things map onto. */
        static final Mask UNMAPPED = new Mask(0, 0, 0, 0, null);

        private final int u;
        private final int v;
        private final int columns;
        private final int rows;
        private final boolean[] covered;

        private Mask(int u, int v, int columns, int rows, boolean[] covered) {
            this.u = u;
            this.v = v;
            this.columns = columns;
            this.rows = rows;
            this.covered = covered;
        }

        boolean none() {
            return this == NONE;
        }

        boolean all() {
            return this == ALL;
        }

        boolean unmapped() {
            return this == UNMAPPED;
        }

        boolean covers(int u, int v) {
            if (covered == null) return this == ALL;
            int column = u - this.u;
            int row = v - this.v;
            return column >= 0 && row >= 0 && column < columns && row < rows && covered[row * columns + column];
        }
    }

    /** One part's coverage, from what is worn over it. */
    static final class Builder {

        private final SkinPart part;
        private final List<Textured> sources = new ArrayList<>();
        private boolean full;

        Builder(SkinPart part) {
            this.part = part;
        }

        /** {@code texture} is drawn on {@code box} over the part; null for something this cannot see into. */
        Builder add(ResourceLocation texture, Box box) {
            if (texture == null) {
                full = true;
            } else {
                sources.add(new Textured(texture, box, opacity(texture)));
            }
            return this;
        }

        Coverage build() {
            if (full) return FULL;
            if (sources.isEmpty()) return NONE;
            Key key = new Key(part, sources);
            Coverage shared = SHARED.get(key);
            if (shared == null) {
                shared = new Coverage(part, key.sources, false);
                SHARED.put(key, shared);
            }
            return shared;
        }
    }

    private static final class Key {

        final SkinPart part;
        final List<Textured> sources;

        Key(SkinPart part, List<Textured> sources) {
            this.part = part;
            this.sources = new ArrayList<>(sources);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key other && part == other.part && sources.equals(other.sources);
        }

        @Override
        public int hashCode() {
            return 31 * part.hashCode() + sources.hashCode();
        }
    }
}
