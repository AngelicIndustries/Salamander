package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemRenderProperties;
import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(Render.class)
public abstract class MixinRenderCemProperties implements CemRenderProperties {

    @Shadow
    protected float shadowSize;
    @Shadow
    protected float shadowOpaque;

    @Override
    public float salamander$shadowSize() {
        return shadowSize;
    }

    @Override
    public float salamander$shadowOpacity() {
        return shadowOpaque;
    }

    @WrapMethod(method = "doRenderShadowAndFire")
    private void salamander$shadowEligibility(Entity entity, double x, double y, double z, float yaw, float partial,
        Operation<Void> original) {
        float previous = shadowSize;
        try {
            shadowSize = (float) CemRuntime.property("render.shadow_size", previous);
            original.call(entity, x, y, z, yaw, partial);
        } finally {
            shadowSize = previous;
        }
    }

    @WrapMethod(method = "renderShadow")
    private void salamander$shadow(Entity entity, double x, double y, double z, float opacity, float partial,
        Operation<Void> original) {
        float previous = shadowSize;
        try {
            shadowSize = (float) CemRuntime.property("render.shadow_size", previous);
            double alpha = CemRuntime.property("render.shadow_opacity", 1);
            original.call(
                entity,
                x + CemRuntime.property("render.shadow_offset_x", 0),
                y,
                z + CemRuntime.property("render.shadow_offset_z", 0),
                (float) (opacity * Math.max(0, Math.min(1, alpha))),
                partial);
        } finally {
            shadowSize = previous;
        }
    }
}
