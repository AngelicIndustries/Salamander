package industries.angelic.salamander.cem.client;

import java.awt.image.BufferedImage;

/** Builds a CEM skin atlas without changing the skin used by the native renderer. */
public final class CemSkinImage {

    private CemSkinImage() {}

    public static BufferedImage copy(BufferedImage source) {
        if (source == null) return null;
        int width = source.getWidth(), height = source.getHeight();
        if (width < 64 || width > 2048 || width % 64 != 0 || height != width && height != width / 2) return null;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, source.getRGB(0, 0, width, height, null, 0, width), 0, width);
        return image;
    }

    /** The texels of a 64x32 skin's head, right leg, body and right arm that the model shows: x1, y1, x2, y2. */
    private static final int[][] SHOWN = { { 8, 0, 24, 8 }, { 0, 8, 32, 16 }, { 4, 16, 12, 20 }, { 0, 20, 16, 32 },
        { 20, 16, 36, 20 }, { 16, 20, 40, 32 }, { 44, 16, 52, 20 }, { 40, 20, 56, 32 } };

    public static BufferedImage normalize(BufferedImage source) {
        BufferedImage image = copy(source);
        if (image == null || image.getHeight() == image.getWidth()) return image;
        int width = image.getWidth(), unit = width / 64;
        int[] pixels = new int[width * width];
        image.getRGB(0, 0, width, width / 2, pixels, 0, width);
        boolean transparentHat = false;
        for (int y = 0; y < 32 * unit; y++)
            for (int x = 32 * unit; x < width; x++) if ((pixels[y * width + x] >>> 24) < 128) transparentHat = true;
        if (!transparentHat) for (int y = 0; y < 32 * unit; y++)
            for (int x = 32 * unit; x < width; x++) pixels[y * width + x] &= 0xffffff;
        // The parts vanilla's model shows are opaque, as vanilla makes them. Texels no part shows keep their alpha, as
        // in 64x64 skins: packs read some, such as Fresh Moves' eyes, which would otherwise come out black.
        for (int[] area : SHOWN) for (int y = area[1] * unit; y < area[3] * unit; y++)
            for (int x = area[0] * unit; x < area[2] * unit; x++) pixels[y * width + x] |= 0xff000000;
        // Each mirrored limb swaps its side faces and reverses every face horizontally.
        int[][] faces = { { 4, 0, 4, 0, 4, 4 }, { 8, 0, 8, 0, 4, 4 }, { 0, 4, 8, 4, 4, 12 }, { 4, 4, 4, 4, 4, 12 },
            { 8, 4, 0, 4, 4, 12 }, { 12, 4, 12, 4, 4, 12 } };
        for (int[] limb : new int[][] { { 0, 16 }, { 40, 32 } }) for (int[] face : faces) {
            for (int y = 0; y < face[5] * unit; y++) for (int x = 0; x < face[4] * unit; x++) {
                int fromX = (limb[0] + face[0] + face[4]) * unit - x - 1;
                int fromY = (16 + face[1]) * unit + y;
                int toX = (limb[1] + face[2]) * unit + x;
                int toY = (48 + face[3]) * unit + y;
                pixels[toY * width + toX] = pixels[fromY * width + fromX];
            }
        }
        BufferedImage result = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, width, pixels, 0, width);
        return result;
    }
}
