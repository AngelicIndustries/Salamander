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
        for (int y = 0; y < 32 * unit; y++)
            for (int x = 0; x < (y < 16 * unit ? 32 * unit : width); x++) pixels[y * width + x] |= 0xff000000;
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
