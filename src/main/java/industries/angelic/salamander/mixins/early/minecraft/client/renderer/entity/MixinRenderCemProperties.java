package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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

    /** EMF's {@code render.fire_x}, {@code _y} and {@code _z} move the burning overlay, in blocks. */
    @Inject(
        method = "renderEntityOnFire",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V",
            ordinal = 0,
            shift = At.Shift.AFTER,
            remap = false))
    private void salamander$fireOffset(Entity entity, double x, double y, double z, float partial, CallbackInfo ci) {
        org.lwjgl.opengl.GL11.glTranslated(
            CemRuntime.property("render.fire_x", 0),
            CemRuntime.property("render.fire_y", 0),
            CemRuntime.property("render.fire_z", 0));
    }

    /** EMF's {@code render.fire_scale}: the width the overlay is sized by, in place of the entity's. */
    @ModifyExpressionValue(
        method = "renderEntityOnFire",
        at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;width:F", opcode = Opcodes.GETFIELD))
    private float salamander$fireWidth(float width) {
        return (float) CemRuntime.property("render.fire_scale", width);
    }

    /** EMF's {@code render.fire_height}: the height the overlay rises to, in place of the entity's. */
    @ModifyExpressionValue(
        method = "renderEntityOnFire",
        at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;height:F", opcode = Opcodes.GETFIELD))
    private float salamander$fireHeight(float height) {
        return (float) CemRuntime.property("render.fire_height", height);
    }
}
