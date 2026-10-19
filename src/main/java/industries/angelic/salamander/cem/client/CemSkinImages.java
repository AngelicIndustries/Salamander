package industries.angelic.salamander.cem.client;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;

/** Transfers original skin pixels from image decoding to the texture, including asynchronous downloads. */
public final class CemSkinImages {

    private static final Map<BufferedImage, BufferedImage> ORIGINALS = new WeakHashMap<>();

    private CemSkinImages() {}

    public static synchronized void remember(BufferedImage processed, BufferedImage original) {
        if (processed != null && original != null && processed != original) ORIGINALS.put(processed, original);
    }

    public static synchronized BufferedImage original(BufferedImage processed) {
        return ORIGINALS.getOrDefault(processed, processed);
    }
}
