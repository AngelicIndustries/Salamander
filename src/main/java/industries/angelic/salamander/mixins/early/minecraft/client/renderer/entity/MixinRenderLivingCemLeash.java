package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.EntityLiving;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(RenderLiving.class)
public abstract class MixinRenderLivingCemLeash {

    @Unique
    private int salamander$vertex;
    @Unique
    private double[] salamander$offset = new double[3];

    @WrapMethod(method = "func_110827_b")
    private void salamander$leash(EntityLiving entity, double x, double y, double z, float yaw, float partial,
        Operation<Void> original) {
        int previous = salamander$vertex;
        double[] previousOffset = salamander$offset;
        salamander$vertex = 0;
        double angle = Math
            .toRadians(entity.prevRenderYawOffset + (entity.renderYawOffset - entity.prevRenderYawOffset) * partial);
        double localX = CemRuntime.property("render.leash_offset_x", 0),
            localZ = CemRuntime.property("render.leash_offset_z", 0);
        salamander$offset = new double[] { localX * Math.cos(angle) - localZ * Math.sin(angle),
            CemRuntime.property("render.leash_offset_y", 0), localX * Math.sin(angle) + localZ * Math.cos(angle) };
        try {
            original.call(entity, x, y, z, yaw, partial);
        } finally {
            salamander$vertex = previous;
            salamander$offset = previousOffset;
        }
    }

    @WrapOperation(
        method = "func_110827_b",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;addVertex(DDD)V"))
    private void salamander$anchor(Tessellator tessellator, double x, double y, double z, Operation<Void> original) {
        double t = (salamander$vertex++ % 50) / 2 / 24d;
        original.call(
            tessellator,
            x + salamander$offset[0] * (1 - t),
            y + salamander$offset[1] * (1 - (t * t + t) * 0.5),
            z + salamander$offset[2] * (1 - t));
    }
}
