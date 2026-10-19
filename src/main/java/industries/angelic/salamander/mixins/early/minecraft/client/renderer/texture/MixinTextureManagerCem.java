package industries.angelic.salamander.mixins.early.minecraft.client.renderer.texture;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import industries.angelic.salamander.cem.client.CemRandomTextures;
import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(TextureManager.class)
public abstract class MixinTextureManagerCem {

    @ModifyVariable(method = "bindTexture", at = @At("HEAD"), argsOnly = true)
    private ResourceLocation salamander$texture(ResourceLocation resource) {
        CemRuntime.texture(resource);
        return CemRandomTextures.resolve(resource);
    }
}
