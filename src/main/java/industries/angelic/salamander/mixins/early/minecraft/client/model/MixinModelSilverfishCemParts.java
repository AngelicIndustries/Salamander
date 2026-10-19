package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSilverfish;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelSilverfish.class)
public abstract class MixinModelSilverfishCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer[] silverfishBodyParts;
    @Shadow
    private ModelRenderer[] silverfishWings;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("body1", silverfishBodyParts[0]);
        parts.put("body2", silverfishBodyParts[1]);
        parts.put("body3", silverfishBodyParts[2]);
        parts.put("body4", silverfishBodyParts[3]);
        parts.put("body5", silverfishBodyParts[4]);
        parts.put("body6", silverfishBodyParts[5]);
        parts.put("body7", silverfishBodyParts[6]);
        parts.put("wing1", silverfishWings[0]);
        parts.put("wing2", silverfishWings[1]);
        parts.put("wing3", silverfishWings[2]);
        return parts;
    }
}
