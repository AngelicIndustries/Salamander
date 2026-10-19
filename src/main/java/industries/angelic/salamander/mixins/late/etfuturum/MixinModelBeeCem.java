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
@Mixin(targets = "ganymedes01.etfuturum.client.model.ModelBee", remap = false)
public abstract class MixinModelBeeCem implements CemModelParts {

    @Shadow
    @Final
    private ModelRenderer body;
    @Shadow
    @Final
    private ModelRenderer torso;
    @Shadow
    @Final
    private ModelRenderer rightWing;
    @Shadow
    @Final
    private ModelRenderer leftWing;
    @Shadow
    @Final
    private ModelRenderer frontLegs;
    @Shadow
    @Final
    private ModelRenderer middleLegs;
    @Shadow
    @Final
    private ModelRenderer backLegs;
    @Shadow
    @Final
    private ModelRenderer stinger;
    @Shadow
    @Final
    private ModelRenderer leftAntenna;
    @Shadow
    @Final
    private ModelRenderer rightAntenna;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("body", body);
        parts.put("torso", torso);
        parts.put("right_wing", rightWing);
        parts.put("left_wing", leftWing);
        parts.put("front_legs", frontLegs);
        parts.put("middle_legs", middleLegs);
        parts.put("back_legs", backLegs);
        parts.put("stinger", stinger);
        parts.put("left_antenna", leftAntenna);
        parts.put("right_antenna", rightAntenna);
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
