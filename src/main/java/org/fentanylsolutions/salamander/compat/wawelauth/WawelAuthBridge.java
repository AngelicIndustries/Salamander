package org.fentanylsolutions.salamander.compat.wawelauth;

import java.util.List;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

import org.fentanylsolutions.wawelauth.api.skin.LayerDraw;
import org.fentanylsolutions.wawelauth.api.skin.LayerSegment;
import org.fentanylsolutions.wawelauth.api.skin.PlayerSkin;
import org.fentanylsolutions.wawelauth.api.skin.PlayerSkins;
import org.fentanylsolutions.wawelauth.api.skin.SkinLayers;
import org.fentanylsolutions.wawelauth.api.skin.SkinPart;
import org.fentanylsolutions.wawelauth.wawelcore.data.SkinModel;

/** Wawel Auth's skin API. Loaded only when a compatible Wawel Auth is present. */
final class WawelAuthBridge {

    private WawelAuthBridge() {}

    static boolean slim(AbstractClientPlayer player) {
        return PlayerSkins.get(player)
            .model() == SkinModel.SLIM;
    }

    static WawelAuthCompat.SkinPixels skin(AbstractClientPlayer player, ResourceLocation texture) {
        PlayerSkin skin = PlayerSkins.get(player);
        if (!skin.available() || !texture.equals(skin.texture())) return null;
        return new WawelAuthCompat.SkinPixels(skin.revision(), skin::originalImage);
    }

    static void claimLayers(List<String> parts) {
        LayerDraw draw = SkinLayers.current();
        if (draw == null) return;
        for (String part : parts) draw.claim(SkinPart.valueOf(part));
    }

    static boolean submit(WawelAuthOverlays.Overlay overlay) {
        LayerDraw draw = SkinLayers.current();
        if (draw == null || !draw.claimed(SkinPart.valueOf(overlay.part))) return false;
        if (overlay.segment == null) overlay.segment = LayerSegment.box(
            SkinPart.valueOf(overlay.part),
            overlay.x,
            overlay.y,
            overlay.z,
            overlay.width,
            overlay.height,
            overlay.depth,
            overlay.u,
            overlay.v);
        draw.submit((LayerSegment) overlay.segment);
        return true;
    }
}
