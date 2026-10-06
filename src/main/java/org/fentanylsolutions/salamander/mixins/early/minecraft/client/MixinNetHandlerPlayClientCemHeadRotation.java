package org.fentanylsolutions.salamander.mixins.early.minecraft.client;

import java.util.Collections;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;

import org.fentanylsolutions.salamander.cem.animation.CemHeadRotation;
import org.fentanylsolutions.salamander.cem.client.CemResources;
import org.fentanylsolutions.salamander.cem.client.CemTargets;
import org.fentanylsolutions.salamander.config.CemConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClientCemHeadRotation {

    @WrapOperation(
        method = "handleEntityHeadLook",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setRotationYawHead(F)V"))
    private void salamander$headTarget(Entity entity, float yaw, Operation<Void> original) {
        String target = CemConfig.enabled && entity instanceof CemHeadRotation.Access ? CemTargets.target(entity)
            : null;
        if (target != null && CemResources.INSTANCE.hasTargets(Collections.singletonList(target))) {
            ((CemHeadRotation.Access) entity).salamander$lerpHeadTo(yaw);
        } else original.call(entity, yaw);
    }
}
