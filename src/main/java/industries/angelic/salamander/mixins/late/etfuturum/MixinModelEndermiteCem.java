package industries.angelic.salamander.mixins.late.etfuturum;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemModelParts;
import industries.angelic.salamander.cem.client.CemRuntime;

@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.client.model.ModelEndermite", remap = false)
public abstract class MixinModelEndermiteCem implements CemModelParts {

    @Shadow
    @Final
    private ModelRenderer[] bodyParts;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("body1", bodyParts.length > 0 ? bodyParts[0] : null);
        parts.put("body2", bodyParts.length > 1 ? bodyParts[1] : null);
        parts.put("body3", bodyParts.length > 2 ? bodyParts[2] : null);
        parts.put("body4", bodyParts.length > 3 ? bodyParts[3] : null);
        return parts;
    }

    @WrapMethod(
        method = { "render(Lnet/minecraft/entity/Entity;FFFFFF)V",
            "func_78088_a(Lnet/minecraft/entity/Entity;FFFFFF)V" },
        remap = false)
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
