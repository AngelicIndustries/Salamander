package org.fentanylsolutions.salamander.compat.wawelauth;

import java.awt.image.BufferedImage;
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
     * Stops Wawel Auth's skin layers in the innermost open draw, the one around the model being replaced. A pack model
     * draws the player's overlays itself.
     */
    public static void suppressLayers() {
        if (LOADED) WawelAuthBridge.suppressLayers();
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
