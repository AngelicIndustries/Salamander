package org.fentanylsolutions.salamander.mixins.early.minecraft.client;

import net.minecraft.entity.EntityLivingBase;

import org.fentanylsolutions.salamander.cem.animation.CemHeadRotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseCemHeadRotation implements CemHeadRotation.Access {

    @Shadow
    public float rotationYawHead;

    @Unique
    private CemHeadRotation salamander$headRotation;

    @Override
    public void salamander$lerpHeadTo(float yaw) {
        if (salamander$headRotation == null) salamander$headRotation = new CemHeadRotation();
        salamander$headRotation.target(yaw);
    }

    @Inject(method = "onEntityUpdate", at = @At("RETURN"))
    private void salamander$turnHead(CallbackInfo ci) {
        // Packets arrive before this tick. Advancing only after prevRotationYawHead is saved
        // prevents vanilla from overwriting the interpolation's starting angle with its target.
        if (salamander$headRotation != null) rotationYawHead = salamander$headRotation.tick(rotationYawHead);
    }

    @Inject(method = "setRotationYawHead", at = @At("HEAD"))
    private void salamander$setHeadImmediately(float yaw, CallbackInfo ci) {
        salamander$headRotation = null;
    }
}
