package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBat;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelBat.class)
public abstract class MixinModelBatCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer batHead;
    @Shadow
    private ModelRenderer batBody;
    @Shadow
    private ModelRenderer batRightWing;
    @Shadow
    private ModelRenderer batLeftWing;
    @Shadow
    private ModelRenderer batOuterRightWing;
    @Shadow
    private ModelRenderer batOuterLeftWing;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", batHead);
        parts.put("body", batBody);
        parts.put("right_wing", batRightWing);
        parts.put("left_wing", batLeftWing);
        parts.put("outer_right_wing", batOuterRightWing);
        parts.put("outer_left_wing", batOuterLeftWing);
        return parts;
    }
}
