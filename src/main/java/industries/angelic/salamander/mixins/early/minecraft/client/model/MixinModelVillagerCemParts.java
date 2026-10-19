package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelVillager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelVillager.class)
public abstract class MixinModelVillagerCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer villagerHead;
    @Shadow
    private ModelRenderer villagerBody;
    @Shadow
    private ModelRenderer villagerArms;
    @Shadow
    private ModelRenderer rightVillagerLeg;
    @Shadow
    private ModelRenderer leftVillagerLeg;
    @Shadow
    private ModelRenderer villagerNose;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", villagerHead);
        parts.put("body", villagerBody);
        parts.put("arms", villagerArms);
        parts.put("right_leg", rightVillagerLeg);
        parts.put("left_leg", leftVillagerLeg);
        parts.put("nose", villagerNose);
        return parts;
    }
}
