package industries.angelic.salamander.mixins.early.minecraft.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelSlime;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(ModelSlime.class)
public abstract class MixinModelSlimeCemRender {

    @WrapMethod(method = "render(Lnet/minecraft/entity/Entity;FFFFFF)V")
    private void salamander$render(Entity entity, float limb, float speed, float age, float yaw, float pitch,
        float scale, Operation<Void> original) {
        CemRuntime.Draw previous = CemRuntime.begin((ModelBase) (Object) this, entity, limb, speed, age, yaw, pitch);
        try {
            original.call(entity, limb, speed, age, yaw, pitch, scale);
        } finally {
            CemRuntime.end(previous, scale);
        }
    }
}
