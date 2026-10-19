package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelMinecart;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelMinecart.class)
public abstract class MixinModelMinecartCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer[] sideModels;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("bottom", sideModels[0]);
        parts.put("back", sideModels[1]);
        parts.put("front", sideModels[2]);
        parts.put("right", sideModels[3]);
        parts.put("left", sideModels[4]);
        parts.put("inside", sideModels[5]);
        return parts;
    }
}
