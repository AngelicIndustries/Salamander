package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.entity.passive.EntityMooshroom;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(net.minecraft.client.renderer.entity.RenderMooshroom.class)
public abstract class MixinRenderMooshroomCem {

    @Inject(
        method = "renderEquippedItems(Lnet/minecraft/entity/passive/EntityMooshroom;F)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glScalef(FFF)V", ordinal = 0, remap = false))
    private void salamander$body(EntityMooshroom entity, float partial, CallbackInfo ci) {
        CemRuntime.accessory("body", .0625f);
    }
}
