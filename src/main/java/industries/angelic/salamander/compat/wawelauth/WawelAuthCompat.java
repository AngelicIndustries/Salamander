package industries.angelic.salamander.compat.wawelauth;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.Loader;

/**
 * Optional Wawel Auth integration. This facade holds no Wawel Auth types; the classes that use its API load only when
 * a compatible Wawel Auth is present.
 */
public final class WawelAuthCompat {

    private static final boolean LOADED = Loader.isModLoaded("wawelauth") && WawelAuthApi.compatible();
    /** CEM part names of the overlay parts, to Wawel Auth's skin part names. */
    private static final Map<String, String> SKIN_PARTS = new HashMap<>();

    static {
        SKIN_PARTS.put("headwear", "HAT");
        SKIN_PARTS.put("jacket", "JACKET");
        SKIN_PARTS.put("left_sleeve", "LEFT_SLEEVE");
        SKIN_PARTS.put("right_sleeve", "RIGHT_SLEEVE");
        SKIN_PARTS.put("left_pants", "LEFT_PANTS");
        SKIN_PARTS.put("right_pants", "RIGHT_PANTS");
    }

    private WawelAuthCompat() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    /** Whether Wawel Auth draws this player slim; null without Wawel Auth. */
    public static Boolean slim(AbstractClientPlayer player) {
        return LOADED ? WawelAuthBridge.slim(player) : null;
    }

    /** The player's skin pixels when the texture is that player's loaded Wawel Auth skin; null otherwise. */
    public static SkinPixels skin(Object subject, ResourceLocation texture) {
        if (!LOADED || !(subject instanceof AbstractClientPlayer) || texture == null) return null;
        return WawelAuthBridge.skin((AbstractClientPlayer) subject, texture);
    }

    /**
     * Takes over the given overlay parts (CEM names, such as {@code jacket}) in the innermost open layer draw, the one
     * around the model being replaced. The pack draws those parts itself, handing vanilla-shaped pieces back through
     * {@link #submit}; Wawel Auth keeps drawing the others on the pack's bones.
     */
    public static void claimLayers(Collection<String> cemParts) {
        if (!LOADED || cemParts.isEmpty()) return;
        List<String> parts = new ArrayList<>();
        for (String cemPart : cemParts) {
            String part = SKIN_PARTS.get(cemPart);
            if (part != null) parts.add(part);
        }
        WawelAuthBridge.claimLayers(parts);
    }

    /**
     * Has Wawel Auth draw a vanilla overlay piece under the current transform, flat or 3D as the viewer chose, if the
     * open layer draw has its part claimed. False when the caller must draw the box itself.
     */
    public static boolean submit(WawelAuthOverlays.Overlay overlay) {
        return LOADED && WawelAuthBridge.submit(overlay);
    }

    /** Skin pixels with a revision that changes whenever they may have. */
    public static final class SkinPixels {

        public final long revision;
        private final Supplier<BufferedImage> image;

        SkinPixels(long revision, Supplier<BufferedImage> image) {
            this.revision = revision;
            this.image = image;
        }

        /** The image as decoded, before Wawel Auth fixed up its alpha. A new copy, or null. */
        public BufferedImage image() {
            return image.get();
        }
    }
}
