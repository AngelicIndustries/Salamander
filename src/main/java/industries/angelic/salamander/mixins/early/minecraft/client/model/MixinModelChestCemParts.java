package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelChest.class)
public abstract class MixinModelChestCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer chestBelow;
    @Shadow
    private ModelRenderer chestLid;
    @Shadow
    private ModelRenderer chestKnob;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("base", chestBelow);
        parts.put("lid", chestLid);
        parts.put("knob", chestKnob);
        return parts;
    }
}
