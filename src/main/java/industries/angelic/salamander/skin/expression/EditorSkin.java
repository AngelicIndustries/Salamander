package industries.angelic.salamander.skin.expression;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

import industries.angelic.salamander.api.skin.PlayerSkin;
import industries.angelic.salamander.api.skin.PlayerSkins;
import industries.angelic.salamander.cem.client.CemSkinImage;
import industries.angelic.salamander.cem.client.CemSkinTexture;

/** The skin the editor starts from: its pixels as decoded, without the expression textures packs generate. */
final class EditorSkin {

    private EditorSkin() {}

    /** A copy of the player's skin; null before it loaded. */
    static BufferedImage of(AbstractClientPlayer player) {
        if (player == null) return null;
        PlayerSkin skin = PlayerSkins.get(player);
        if (skin.available() && skin.originalImage() != null) return CemSkinImage.copy(skin.originalImage());
        return CemSkinImage.copy(image(player.getLocationSkin()));
    }

    /** The decoded pixels of a loaded skin texture, or of a default skin from the resource packs. */
    private static BufferedImage image(ResourceLocation location) {
        if (location == null) return null;
        ITextureObject texture = Minecraft.getMinecraft()
            .getTextureManager()
            .getTexture(location);
        if (texture instanceof CemSkinTexture skin) {
            if (skin.salamander$skinImage() != null) return skin.salamander$skinImage();
            if (skin.salamander$uploadedImage() != null) return skin.salamander$uploadedImage();
        }
        try (InputStream input = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(location)
            .getInputStream()) {
            return ImageIO.read(input);
        } catch (IOException | RuntimeException noResource) {
            return null;
        }
    }
}
