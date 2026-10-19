package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelBiped.class)
public abstract class MixinModelBipedCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer bipedHead;
    @Shadow
    private ModelRenderer bipedHeadwear;
    @Shadow
    private ModelRenderer bipedBody;
    @Shadow
    private ModelRenderer bipedRightArm;
    @Shadow
    private ModelRenderer bipedLeftArm;
    @Shadow
    private ModelRenderer bipedRightLeg;
    @Shadow
    private ModelRenderer bipedLeftLeg;
    @Shadow
    private ModelRenderer bipedEars;
    @Shadow
    private ModelRenderer bipedCloak;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", bipedHead);
        parts.put("headwear", bipedHeadwear);
        parts.put("body", bipedBody);
        parts.put("right_arm", bipedRightArm);
        parts.put("left_arm", bipedLeftArm);
        parts.put("right_leg", bipedRightLeg);
        parts.put("left_leg", bipedLeftLeg);
        parts.put("ear", bipedEars);
        parts.put("cloak", bipedCloak);
        return parts;
    }
}
