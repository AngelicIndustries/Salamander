package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBoat;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelBoat.class)
public abstract class MixinModelBoatCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer[] boatSides;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("bottom", boatSides[0]);
        parts.put("back", boatSides[1]);
        parts.put("front", boatSides[2]);
        parts.put("right", boatSides[3]);
        parts.put("left", boatSides[4]);
        return parts;
    }
}
