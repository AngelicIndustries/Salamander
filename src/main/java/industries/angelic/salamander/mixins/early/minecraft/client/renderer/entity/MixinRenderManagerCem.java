package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemEntityInputs;
import industries.angelic.salamander.cem.client.CemResources;
import industries.angelic.salamander.cem.client.CemRuntime;
import industries.angelic.salamander.cem.client.CemTargets;
import industries.angelic.salamander.config.CemConfig;

@Mixin(RenderManager.class)
public abstract class MixinRenderManagerCem {

    @WrapMethod(method = "func_147939_a")
    private boolean salamander$scope(Entity entity, double x, double y, double z, float yaw, float partial,
        boolean debug, Operation<Boolean> original) {
        CemRuntime.Subject previous = CemRuntime.enter(entity, partial);
        EntityLivingBase living = entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
        float previousBodyYaw = living == null ? 0 : living.prevRenderYawOffset;
        try {
            if (living instanceof CemEntityInputs && CemConfig.enabled
                && CemResources.INSTANCE.hasTargets(java.util.Collections.singletonList(CemTargets.target(entity))))
                living.prevRenderYawOffset = ((CemEntityInputs) living).salamander$cemPreviousBodyYaw(previousBodyYaw);
            return original.call(entity, x, y, z, yaw, partial, debug);
        } finally {
            if (living != null) living.prevRenderYawOffset = previousBodyYaw;
            CemRuntime.leave(previous);
        }
    }
}
