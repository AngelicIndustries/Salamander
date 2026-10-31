package industries.angelic.salamander.skin.source;

import java.awt.image.BufferedImage;

/**
 * Parses a downloaded skin the way 1.8 does, for Salamander's 64x64 player model when Wawel Auth is not there to do
 * it: legacy 64x32 skins get their left limbs mirrored from the right, base parts are made opaque, and only legacy
 * skins get the hat transparency hack. HD skins wider than 64 pass through. Pure raster: no AWT Graphics.
 */
public final class ModernSkinImage {

    private ModernSkinImage() {}

    public static BufferedImage parse(BufferedImage image) {
        if (image == null) return null;
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > 64) return argb(image);

        int[] data = new int[64 * 64];
        boolean legacy = height <= 32;
        copy(image, data, Math.min(width, 64), Math.min(height, legacy ? 32 : 64));
        if (legacy) {
            mirrorLimb(data, 40, 16, 32, 48);
            mirrorLimb(data, 0, 16, 16, 48);
        }
        setOpaque(data, 0, 0, 32, 16);
        if (legacy) setTransparent(data, 32, 0, 64, 32);
        setOpaque(data, 0, 16, 64, 32);
        setOpaque(data, 16, 48, 48, 64);

        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, 64, 64, data, 0, 64);
        return out;
    }

    private static BufferedImage argb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) return image;
        int width = image.getWidth(), height = image.getHeight();
        BufferedImage converted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        converted.setRGB(0, 0, width, height, image.getRGB(0, 0, width, height, null, 0, width), 0, width);
        return converted;
    }

    private static void copy(BufferedImage source, int[] data, int width, int height) {
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            source.getRGB(0, y, width, 1, row, 0, width);
            System.arraycopy(row, 0, data, y * 64, width);
        }
    }

    /**
     * A right limb's 4x12x4 block mirrored onto the left limb's: each face flipped, and the inner and outer sides
     * swapped, as 1.8 converts legacy skins.
     */
    private static void mirrorLimb(int[] data, int fromX, int fromY, int toX, int toY) {
        mirrorFace(data, fromX + 4, fromY, toX + 4, toY, 4, 4);
        mirrorFace(data, fromX + 8, fromY, toX + 8, toY, 4, 4);
        mirrorFace(data, fromX, fromY + 4, toX + 8, toY + 4, 4, 12);
        mirrorFace(data, fromX + 4, fromY + 4, toX + 4, toY + 4, 4, 12);
        mirrorFace(data, fromX + 8, fromY + 4, toX, toY + 4, 4, 12);
        mirrorFace(data, fromX + 12, fromY + 4, toX + 12, toY + 4, 4, 12);
    }

    private static void mirrorFace(int[] data, int fromX, int fromY, int toX, int toY, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[(toY + y) * 64 + toX + x] = data[(fromY + y) * 64 + fromX + width - 1 - x];
            }
        }
    }

    private static void setOpaque(int[] data, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) data[y * 64 + x] |= 0xFF000000;
        }
    }

    /** Vanilla's hack: clear the hat area only if it has no transparent pixel at all. */
    private static void setTransparent(int[] data, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if ((data[y * 64 + x] >>> 24) < 128) return;
            }
        }
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) data[y * 64 + x] &= 0x00FFFFFF;
        }
    }
}
