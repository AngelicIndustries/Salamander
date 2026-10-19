package industries.angelic.salamander.mixins.early.minecraft.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelDragon;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(ModelDragon.class)
public abstract class MixinModelDragonCemRender {

    @WrapMethod(method = "render(Lnet/minecraft/entity/Entity;FFFFFF)V")
    private void salamander$render(Entity entity, float limb, float speed, float age, float yaw, float pitch,
        float scale, Operation<Void> original) {
        CemRuntime.Draw previous = CemRuntime.begin((ModelBase) (Object) this, entity, limb, speed, age, yaw, pitch);
        try {
            if (CemRuntime.collectDragonPose(true)) {
                org.lwjgl.opengl.GL11
                    .glPushAttrib(org.lwjgl.opengl.GL11.GL_ENABLE_BIT | org.lwjgl.opengl.GL11.GL_POLYGON_BIT);
                try {
                    original.call(entity, limb, speed, age, yaw, pitch, scale);
                } finally {
                    CemRuntime.collectDragonPose(false);
                    org.lwjgl.opengl.GL11.glPopAttrib();
                }
            }
            original.call(entity, limb, speed, age, yaw, pitch, scale);
        } finally {
            CemRuntime.end(previous, scale);
        }
    }
}
