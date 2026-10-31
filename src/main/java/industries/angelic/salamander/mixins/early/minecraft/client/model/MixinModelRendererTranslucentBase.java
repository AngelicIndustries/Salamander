package industries.angelic.salamander.mixins.early.minecraft.client.model;

import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import industries.angelic.salamander.skin.translucent.TranslucentLayers;

/** Splits the semi-transparent texels off a see-through player's base, for model parts drawn from display lists. */
@Mixin(ModelRenderer.class)
public abstract class MixinModelRendererTranslucentBase {

    @WrapOperation(
        method = { "render", "renderWithRotation" },
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glCallList(I)V", remap = false))
    private void salamander$splitSeeThroughBase(int list, Operation<Void> original) {
        if (!TranslucentLayers.splitsBase()) {
            original.call(list);
            return;
        }
        TranslucentLayers.splitBase(list, () -> original.call(list));
    }
}
