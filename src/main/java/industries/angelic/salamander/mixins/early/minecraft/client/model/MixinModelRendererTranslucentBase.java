package industries.angelic.salamander.mixins.early.minecraft.client.model;

import net.minecraft.client.model.ModelRenderer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.WrapWithCondition;

import industries.angelic.salamander.skin.translucent.TranslucentLayers;

/**
 * Splits the semi-transparent texels off a see-through player's base, for model parts drawn from display lists. Every
 * model part of every entity passes here, so the usual case is a plain condition that allocates nothing.
 */
@Mixin(ModelRenderer.class)
public abstract class MixinModelRendererTranslucentBase {

    @WrapWithCondition(
        method = { "render", "renderWithRotation" },
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glCallList(I)V", remap = false))
    private boolean salamander$splitSeeThroughBase(int list) {
        if (!TranslucentLayers.splitsBase()) return true;
        TranslucentLayers.splitBase(list, () -> GL11.glCallList(list));
        return false;
    }
}
