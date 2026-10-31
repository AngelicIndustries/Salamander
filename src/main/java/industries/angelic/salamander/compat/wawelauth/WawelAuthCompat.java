package industries.angelic.salamander.compat.wawelauth;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.Loader;
import industries.angelic.salamander.api.skin.PlayerSkins;

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

    /** Wawel Auth's skins for Salamander's player rendering; null without a compatible Wawel Auth. */
    public static PlayerSkins.Source skins() {
        return LOADED ? new WawelAuthSkins() : null;
    }

    /** The player's skin pixels when the texture is that player's loaded Wawel Auth skin; null otherwise. */
    public static SkinPixels skin(Object subject, ResourceLocation texture) {
        if (!LOADED || !(subject instanceof AbstractClientPlayer) || texture == null) return null;
        return WawelAuthBridge.skin((AbstractClientPlayer) subject, texture);
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
