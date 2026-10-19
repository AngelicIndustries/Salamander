package industries.angelic.salamander.mixins.late.etfuturum;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
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
@Mixin(targets = "ganymedes01.etfuturum.client.model.ModelRabbit", remap = false)
public abstract class MixinModelRabbitCem implements CemModelParts {

    @Shadow
    private ModelRenderer rabbitLeftFoot;
    @Shadow
    private ModelRenderer rabbitRightFoot;
    @Shadow
    private ModelRenderer rabbitLeftThigh;
    @Shadow
    private ModelRenderer rabbitRightThigh;
    @Shadow
    private ModelRenderer rabbitBody;
    @Shadow
    private ModelRenderer rabbitLeftArm;
    @Shadow
    private ModelRenderer rabbitRightArm;
    @Shadow
    private ModelRenderer rabbitHead;
    @Shadow
    private ModelRenderer rabbitRightEar;
    @Shadow
    private ModelRenderer rabbitLeftEar;
    @Shadow
    private ModelRenderer rabbitTail;
    @Shadow
    private ModelRenderer rabbitNose;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("left_foot", rabbitLeftFoot);
        parts.put("right_foot", rabbitRightFoot);
        parts.put("left_thigh", rabbitLeftThigh);
        parts.put("right_thigh", rabbitRightThigh);
        parts.put("body", rabbitBody);
        parts.put("left_arm", rabbitLeftArm);
        parts.put("right_arm", rabbitRightArm);
        parts.put("head", rabbitHead);
        parts.put("right_ear", rabbitRightEar);
        parts.put("left_ear", rabbitLeftEar);
        parts.put("tail", rabbitTail);
        parts.put("nose", rabbitNose);
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
