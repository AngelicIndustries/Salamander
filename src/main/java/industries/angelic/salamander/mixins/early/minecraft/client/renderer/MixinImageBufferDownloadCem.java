package industries.angelic.salamander.mixins.early.minecraft.client.renderer;

import java.awt.image.BufferedImage;

import net.minecraft.client.renderer.ImageBufferDownload;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemSkinImage;
import industries.angelic.salamander.cem.client.CemSkinImages;

@Mixin(ImageBufferDownload.class)
public abstract class MixinImageBufferDownloadCem {

    @WrapMethod(method = "parseUserSkin")
    private BufferedImage salamander$preserveSkin(BufferedImage image, Operation<BufferedImage> original) {
        BufferedImage preserved = CemSkinImage.copy(image);
        BufferedImage processed = original.call(image);
        CemSkinImages.remember(processed, preserved);
        return processed;
    }
}
