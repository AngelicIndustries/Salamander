package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelVillager;
import net.minecraft.client.model.ModelWitch;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelWitch.class)
public abstract class MixinModelWitchCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer witchHat;
    @Shadow
    private ModelRenderer field_82901_h;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        ModelVillager model = (ModelVillager) (Object) this;
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", model.villagerHead);
        parts.put("body", model.villagerBody);
        parts.put("arms", model.villagerArms);
        parts.put("right_leg", model.rightVillagerLeg);
        parts.put("left_leg", model.leftVillagerLeg);
        parts.put("nose", model.villagerNose);
        parts.put("headwear", witchHat);
        parts.put("mole", field_82901_h);
        return parts;
    }
}
