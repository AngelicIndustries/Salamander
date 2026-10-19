package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelIronGolem;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelIronGolem.class)
public abstract class MixinModelIronGolemCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer ironGolemHead;
    @Shadow
    private ModelRenderer ironGolemBody;
    @Shadow
    private ModelRenderer ironGolemRightArm;
    @Shadow
    private ModelRenderer ironGolemLeftArm;
    @Shadow
    private ModelRenderer ironGolemRightLeg;
    @Shadow
    private ModelRenderer ironGolemLeftLeg;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", ironGolemHead);
        parts.put("body", ironGolemBody);
        parts.put("right_arm", ironGolemRightArm);
        parts.put("left_arm", ironGolemLeftArm);
        parts.put("right_leg", ironGolemRightLeg);
        parts.put("left_leg", ironGolemLeftLeg);
        return parts;
    }
}
