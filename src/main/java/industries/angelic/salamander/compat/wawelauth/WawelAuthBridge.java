package industries.angelic.salamander.compat.wawelauth;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

import industries.angelic.wawelauth.api.skin.PlayerSkin;
import industries.angelic.wawelauth.api.skin.PlayerSkins;

/** Wawel Auth's skin API. Loaded only when a compatible Wawel Auth is present. */
final class WawelAuthBridge {

    private WawelAuthBridge() {}

    static WawelAuthCompat.SkinPixels skin(AbstractClientPlayer player, ResourceLocation texture) {
        PlayerSkin skin = PlayerSkins.get(player);
        if (!skin.available() || !texture.equals(skin.texture())) return null;
        return new WawelAuthCompat.SkinPixels(skin.revision(), skin::originalImage);
    }
}
