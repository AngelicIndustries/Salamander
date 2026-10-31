package industries.angelic.salamander.mixins.early.minecraft.client.renderer;

import java.awt.image.BufferedImage;

import net.minecraft.client.renderer.ImageBufferDownload;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemSkinImage;
import industries.angelic.salamander.cem.client.CemSkinImages;
import industries.angelic.salamander.compat.ears.EarsDetect;
import industries.angelic.salamander.compat.wawelauth.WawelAuthCompat;
import industries.angelic.salamander.skin.source.ModernSkinImage;

@Mixin(ImageBufferDownload.class)
public abstract class MixinImageBufferDownloadCem {

    /**
     * Keeps the decoded pixels for CEM. Without Wawel Auth, which parses skins to 64x64 itself, Salamander's 64x64
     * player model needs the modern layout too; with Ears drawing players, Ears parses them.
     */
    @WrapMethod(method = "parseUserSkin")
    private BufferedImage salamander$preserveSkin(BufferedImage image, Operation<BufferedImage> original) {
        BufferedImage preserved = CemSkinImage.copy(image);
        BufferedImage processed = WawelAuthCompat.isLoaded() || EarsDetect.isRendererActive() ? original.call(image)
            : ModernSkinImage.parse(image);
        CemSkinImages.remember(processed, preserved);
        return processed;
    }
}
