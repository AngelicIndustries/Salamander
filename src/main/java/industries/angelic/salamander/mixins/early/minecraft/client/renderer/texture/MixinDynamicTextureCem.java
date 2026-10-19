package industries.angelic.salamander.mixins.early.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;

import net.minecraft.client.renderer.texture.DynamicTexture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.client.CemSkinTexture;

@Mixin(DynamicTexture.class)
public abstract class MixinDynamicTextureCem implements CemSkinTexture {

    @Shadow
    private int width;
    @Shadow
    private int height;

    @Shadow
    public abstract int[] getTextureData();

    @Unique
    private BufferedImage salamander$skinSnapshot;

    @Inject(method = "updateDynamicTexture", at = @At("HEAD"))
    private void salamander$invalidate(CallbackInfo ci) {
        salamander$skinSnapshot = null;
    }

    @Override
    public BufferedImage salamander$skinImage() {
        if (width < 64 || width > 2048 || width % 64 != 0 || height != width && height != width / 2) return null;
        if (salamander$skinSnapshot == null) {
            salamander$skinSnapshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            salamander$skinSnapshot.setRGB(0, 0, width, height, getTextureData(), 0, width);
        }
        return salamander$skinSnapshot;
    }
}
