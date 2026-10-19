package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelMagmaCube;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelMagmaCube.class)
public abstract class MixinModelMagmaCubeCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer field_78108_b;
    @Shadow
    private ModelRenderer[] field_78109_a;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("core", field_78108_b);
        parts.put("segment1", field_78109_a[0]);
        parts.put("segment2", field_78109_a[1]);
        parts.put("segment3", field_78109_a[2]);
        parts.put("segment4", field_78109_a[3]);
        parts.put("segment5", field_78109_a[4]);
        parts.put("segment6", field_78109_a[5]);
        parts.put("segment7", field_78109_a[6]);
        parts.put("segment8", field_78109_a[7]);
        return parts;
    }
}
