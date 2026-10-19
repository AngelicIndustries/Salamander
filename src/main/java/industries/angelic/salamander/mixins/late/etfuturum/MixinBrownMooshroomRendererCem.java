package industries.angelic.salamander.mixins.late.etfuturum;

import net.minecraft.entity.passive.EntityMooshroom;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.client.CemRuntime;

@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "ganymedes01.etfuturum.client.renderer.entity.BrownMooshroomRenderer", remap = false)
public abstract class MixinBrownMooshroomRendererCem {

    @Inject(
        method = { "renderEquippedItems(Lnet/minecraft/entity/passive/EntityMooshroom;F)V",
            "func_77029_c(Lnet/minecraft/entity/passive/EntityMooshroom;F)V" },
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glScalef(FFF)V", ordinal = 0, remap = false),
        remap = false)
    private void salamander$body(EntityMooshroom entity, float partial, CallbackInfo ci) {
        CemRuntime.accessory("body", .0625f);
    }
}
