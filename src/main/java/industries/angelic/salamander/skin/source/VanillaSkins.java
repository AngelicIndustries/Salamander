package industries.angelic.salamander.skin.source;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.api.skin.PlayerSkin;
import industries.angelic.salamander.api.skin.PlayerSkins;
import industries.angelic.salamander.api.skin.SkinPart;
import industries.angelic.salamander.cem.client.CemPlayers;
import industries.angelic.salamander.cem.client.CemSkinTexture;

/**
 * Skins from vanilla's own downloads, for when Wawel Auth is not loaded. Downloads are parsed to 64x64 by
 * {@link ModernSkinImage}; slim arms come from the texture metadata SkinManager reports or the profile property.
 * Vanilla has no per-part toggles, so every part shows.
 */
@SideOnly(Side.CLIENT)
final class VanillaSkins implements PlayerSkins.Source {

    private final RevisionTracker<UUID, Key, PlayerSkin> tracker = new RevisionTracker<>(512);
    private final Map<ResourceLocation, BufferedImage> resources = new HashMap<>();

    @Override
    public PlayerSkin get(AbstractClientPlayer player) {
        UUID profileId = player.getUniqueID();
        ResourceLocation texture = player.getLocationSkin();
        ITextureObject object = Minecraft.getMinecraft()
            .getTextureManager()
            .getTexture(texture);
        BufferedImage uploaded = uploaded(texture, object);
        BufferedImage original = object instanceof CemSkinTexture skin && skin.salamander$skinImage() != null
            ? skin.salamander$skinImage()
            : uploaded;
        boolean slim = slim(player);
        return tracker.get(
            profileId,
            new Key(texture, object, uploaded, slim),
            revision -> new Snapshot(profileId, revision, texture, slim, original, uploaded));
    }

    @Override
    public boolean slim(AbstractClientPlayer player) {
        return CemPlayers.profileSlim(player);
    }

    @Override
    public boolean isShown(EntityPlayer player, SkinPart part) {
        return true;
    }

    @Override
    public ResourceLocation skull(GameProfile profile) {
        if (profile == null) return null;
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = Minecraft.getMinecraft()
            .func_152342_ad()
            .func_152788_a(profile);
        MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
        return skin == null ? null
            : Minecraft.getMinecraft()
                .func_152342_ad()
                .func_152792_a(skin, MinecraftProfileTexture.Type.SKIN);
    }

    @Override
    public BufferedImage image(ResourceLocation texture) {
        ITextureObject object = Minecraft.getMinecraft()
            .getTextureManager()
            .getTexture(texture);
        return object == null ? null : uploaded(texture, object);
    }

    private BufferedImage uploaded(ResourceLocation texture, ITextureObject object) {
        if (object instanceof CemSkinTexture skin) return skin.salamander$uploadedImage();
        return object != null ? resource(texture) : null;
    }

    /** Default skins come from resource packs. Remembered per location, including failures. */
    private BufferedImage resource(ResourceLocation texture) {
        if (resources.containsKey(texture)) return resources.get(texture);
        BufferedImage image = null;
        try (InputStream input = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(texture)
            .getInputStream()) {
            image = ModernSkinImage.parse(ImageIO.read(input));
        } catch (Exception ignored) {
            // Not a resource texture, or a pack without it: no pixels for this skin.
        }
        resources.put(texture, image);
        return image;
    }

    /** What a snapshot was built from. Texture objects and images compare by identity. */
    private static final class Key {

        private final ResourceLocation texture;
        private final ITextureObject object;
        private final BufferedImage image;
        private final boolean slim;

        Key(ResourceLocation texture, ITextureObject object, BufferedImage image, boolean slim) {
            this.texture = texture;
            this.object = object;
            this.image = image;
            this.slim = slim;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key other && object == other.object
                && image == other.image
                && slim == other.slim
                && (texture == null ? other.texture == null : texture.equals(other.texture));
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(object) * 31 + System.identityHashCode(image);
        }
    }

    private static final class Snapshot implements PlayerSkin {

        private final UUID profileId;
        private final long revision;
        private final ResourceLocation texture;
        private final boolean slim;
        private final BufferedImage original;
        private final BufferedImage normalized;

        Snapshot(UUID profileId, long revision, ResourceLocation texture, boolean slim, BufferedImage original,
            BufferedImage normalized) {
            this.profileId = profileId;
            this.revision = revision;
            this.texture = texture;
            this.slim = slim;
            this.original = original;
            this.normalized = normalized;
        }

        @Override
        public UUID profileId() {
            return profileId;
        }

        @Override
        public long revision() {
            return revision;
        }

        @Override
        public boolean available() {
            return normalized != null;
        }

        @Override
        public ResourceLocation texture() {
            return texture;
        }

        @Override
        public boolean slim() {
            return slim;
        }

        @Override
        public BufferedImage originalImage() {
            return copy(original);
        }

        @Override
        public BufferedImage normalizedImage() {
            return copy(normalized);
        }

        private static BufferedImage copy(BufferedImage image) {
            if (image == null) return null;
            int width = image.getWidth(), height = image.getHeight();
            BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            copy.setRGB(0, 0, width, height, image.getRGB(0, 0, width, height, null, 0, width), 0, width);
            return copy;
        }
    }
}
