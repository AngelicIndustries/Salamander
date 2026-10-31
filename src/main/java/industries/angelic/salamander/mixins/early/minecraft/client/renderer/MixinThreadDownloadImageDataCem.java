package industries.angelic.salamander.mixins.early.minecraft.client.renderer;

import java.awt.image.BufferedImage;

import net.minecraft.client.renderer.ThreadDownloadImageData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.client.CemSkinImages;
import industries.angelic.salamander.cem.client.CemSkinTexture;

@Mixin(ThreadDownloadImageData.class)
public abstract class MixinThreadDownloadImageDataCem implements CemSkinTexture {

    @Unique
    private volatile BufferedImage salamander$originalSkin;

    @Unique
    private volatile BufferedImage salamander$uploadedSkin;

    @Inject(method = "setBufferedImage", at = @At("HEAD"))
    private void salamander$captureSkin(BufferedImage image, CallbackInfo ci) {
        salamander$originalSkin = CemSkinImages.original(image);
        salamander$uploadedSkin = image;
    }

    @Override
    public BufferedImage salamander$skinImage() {
        return salamander$originalSkin;
    }

    @Override
    public BufferedImage salamander$uploadedImage() {
        return salamander$uploadedSkin;
    }
}
