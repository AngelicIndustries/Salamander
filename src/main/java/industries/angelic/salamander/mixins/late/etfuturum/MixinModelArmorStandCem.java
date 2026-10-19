package industries.angelic.salamander.mixins.late.etfuturum;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemModelParts;
import industries.angelic.salamander.cem.client.CemRuntime;

@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.client.model.ModelArmorStand", remap = false)
public abstract class MixinModelArmorStandCem implements CemModelParts {

    @Shadow
    private ModelRenderer standRightSide;
    @Shadow
    private ModelRenderer standLeftSide;
    @Shadow
    private ModelRenderer standWaist;
    @Shadow
    private ModelRenderer standBase;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("right", standRightSide);
        parts.put("left", standLeftSide);
        parts.put("waist", standWaist);
        parts.put("base", standBase);
        parts.put("head", ((ModelBiped) (Object) this).bipedHead);
        parts.put("headwear", ((ModelBiped) (Object) this).bipedHeadwear);
        parts.put("body", ((ModelBiped) (Object) this).bipedBody);
        parts.put("right_arm", ((ModelBiped) (Object) this).bipedRightArm);
        parts.put("left_arm", ((ModelBiped) (Object) this).bipedLeftArm);
        parts.put("right_leg", ((ModelBiped) (Object) this).bipedRightLeg);
        parts.put("left_leg", ((ModelBiped) (Object) this).bipedLeftLeg);
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
