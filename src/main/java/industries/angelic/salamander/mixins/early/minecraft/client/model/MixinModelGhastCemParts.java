package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelGhast;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelGhast.class)
public abstract class MixinModelGhastCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer body;
    @Shadow
    private ModelRenderer[] tentacles;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("body", body);
        parts.put("tentacle1", tentacles[0]);
        parts.put("tentacle2", tentacles[1]);
        parts.put("tentacle3", tentacles[2]);
        parts.put("tentacle4", tentacles[3]);
        parts.put("tentacle5", tentacles[4]);
        parts.put("tentacle6", tentacles[5]);
        parts.put("tentacle7", tentacles[6]);
        parts.put("tentacle8", tentacles[7]);
        parts.put("tentacle9", tentacles[8]);
        return parts;
    }
}
