package industries.angelic.salamander.mixins.late.etfuturum;

import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import industries.angelic.salamander.compat.etfuturum.ElytraBodyFrame;

/** In a bent body's frame (see ElytraBodyFrame) the back already crouches: keep the wings' standing pose. */
@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.client.renderer.entity.elytra.ModelElytra", remap = false)
public abstract class MixinModelElytraBody {

    @ModifyExpressionValue(
        method = { "setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V",
            "func_78087_a(FFFFFFLnet/minecraft/entity/Entity;)V" },
        at = { @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z"),
            @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;func_70093_af()Z") },
        require = 0)
    private boolean salamander$standingInBodyFrame(boolean sneaking, float limbSwing, float limbSwingAmount,
        float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        return ElytraBodyFrame.crouches(sneaking, entity);
    }
}
