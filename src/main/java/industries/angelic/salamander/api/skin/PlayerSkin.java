package industries.angelic.salamander.api.skin;

import java.awt.image.BufferedImage;
import java.util.UUID;

import net.minecraft.util.ResourceLocation;

/**
 * A player's skin as Salamander draws it, from {@link PlayerSkins#get}. Immutable.
 * <p>
 * The revision changes whenever the skin's pixels or arms may have changed. Equal revisions mean equal pixels, so
 * caches can key on {@code (profileId, revision)}. Revisions are unique across players.
 */
public interface PlayerSkin {

    UUID profileId();

    long revision();

    /** False while the skin is still downloading or could not be loaded; the texture is then a placeholder. */
    boolean available();

    /** The texture bound when drawing this player. */
    ResourceLocation texture();

    boolean slim();

    /** The image as decoded, before its alpha was fixed up. A new copy owned by the caller; null when not available. */
    BufferedImage originalImage();

    /** The image as uploaded: 64x64, or square HD. A new copy owned by the caller; null when not available. */
    BufferedImage normalizedImage();
}
