package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderWitch;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(RenderWitch.class)
public abstract class MixinRenderWitchCem {

    /** The witch's potion goes to the pack model's {@code witch_item} attachment instead of the nose. */
    @WrapOperation(
        method = "renderEquippedItems(Lnet/minecraft/entity/monster/EntityWitch;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;postRender(F)V", ordinal = 0))
    private void salamander$witchItem(ModelRenderer nose, float scale, Operation<Void> original) {
        if (!CemRuntime.attach("witch_item", scale)) original.call(nose, scale);
    }
}
