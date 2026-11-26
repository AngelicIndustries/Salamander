package industries.angelic.salamander.mixins.late.etfuturum;

import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.compat.etfuturum.ElytraBodyFrame;

/**
 * Et Futurum draws the elytra at the model's root with vanilla's crouch pose: on a body another mod bends (Even Mo'
 * Bends) it sticks out where vanilla's back would be. Draw it in that body's frame instead, without the crouch pose.
 */
@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.client.renderer.entity.elytra.LayerBetterElytra", remap = false)
public abstract class MixinLayerBetterElytraBody {

    @Inject(
        method = "doRenderLayer",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPushMatrix()V", shift = At.Shift.AFTER),
        require = 0)
    private static void salamander$bodyFrame(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float scale, CallbackInfo ci) {
        ElytraBodyFrame.begin(entity, scale);
    }

    @Inject(method = "doRenderLayer", at = @At("RETURN"), require = 0)
    private static void salamander$bodyFrameDone(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float scale, CallbackInfo ci) {
        ElytraBodyFrame.end();
    }
}
