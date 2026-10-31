package industries.angelic.salamander.compat.wawelauth;

import java.awt.image.BufferedImage;
import java.util.UUID;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.mojang.authlib.GameProfile;

import industries.angelic.salamander.api.skin.PlayerSkin;
import industries.angelic.salamander.api.skin.PlayerSkins;
import industries.angelic.salamander.api.skin.SkinPart;
import industries.angelic.wawelauth.wawelcore.data.SkinModel;

/** Skins as Wawel Auth supplies them. Loaded only when a compatible Wawel Auth is present. */
final class WawelAuthSkins implements PlayerSkins.Source {

    @Override
    public PlayerSkin get(AbstractClientPlayer player) {
        return new Snapshot(industries.angelic.wawelauth.api.skin.PlayerSkins.get(player));
    }

    @Override
    public boolean slim(AbstractClientPlayer player) {
        return industries.angelic.wawelauth.api.skin.PlayerSkins.get(player)
            .model() == SkinModel.SLIM;
    }

    @Override
    public boolean isShown(EntityPlayer player, SkinPart part) {
        return industries.angelic.wawelauth.api.skin.PlayerSkins
            .isShown(player, industries.angelic.wawelauth.api.skin.SkinPart.valueOf(part.name()));
    }

    @Override
    public ResourceLocation skull(GameProfile profile) {
        return industries.angelic.wawelauth.api.skin.PlayerSkins.skull(profile);
    }

    @Override
    public BufferedImage image(ResourceLocation texture) {
        return industries.angelic.wawelauth.api.skin.PlayerSkins.image(texture);
    }

    private static final class Snapshot implements PlayerSkin {

        private final industries.angelic.wawelauth.api.skin.PlayerSkin skin;

        Snapshot(industries.angelic.wawelauth.api.skin.PlayerSkin skin) {
            this.skin = skin;
        }

        @Override
        public UUID profileId() {
            return skin.profileId();
        }

        @Override
        public long revision() {
            return skin.revision();
        }

        @Override
        public boolean available() {
            return skin.available();
        }

        @Override
        public ResourceLocation texture() {
            return skin.texture();
        }

        @Override
        public boolean slim() {
            return skin.model() == SkinModel.SLIM;
        }

        @Override
        public BufferedImage originalImage() {
            return skin.originalImage();
        }

        @Override
        public BufferedImage normalizedImage() {
            return skin.normalizedImage();
        }
    }
}
