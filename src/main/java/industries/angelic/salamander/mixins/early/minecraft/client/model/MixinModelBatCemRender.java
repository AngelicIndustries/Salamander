package industries.angelic.salamander.mixins.early.minecraft.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBat;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(ModelBat.class)
public abstract class MixinModelBatCemRender {

    @WrapMethod(method = "render(Lnet/minecraft/entity/Entity;FFFFFF)V")
    private void salamander$render(Entity entity, float limb, float speed, float age, float yaw, float pitch,
        float scale, Operation<Void> original) {
        CemRuntime.Draw previous = CemRuntime.begin((ModelBase) (Object) this, entity, limb, speed, age, yaw, pitch);
        boolean modern = CemRuntime.modernBat();
        // The remodeled bat uses pixel-sized geometry; the old renderer scales its oversized geometry by .35.
        if (modern) {
            org.lwjgl.opengl.GL11.glPushMatrix();
            org.lwjgl.opengl.GL11.glTranslatef(0, 1.5078125F, 0);
            org.lwjgl.opengl.GL11.glScalef(1F / .35F, 1F / .35F, 1F / .35F);
            org.lwjgl.opengl.GL11.glTranslatef(0, -1.5078125F, 0);
            CemRuntime.recaptureOrigin();
        }
        try {
            original.call(entity, limb, speed, age, yaw, pitch, scale);
        } finally {
            try {
                CemRuntime.end(previous, scale);
            } finally {
                if (modern) org.lwjgl.opengl.GL11.glPopMatrix();
            }
        }
    }
}
