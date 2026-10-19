package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBlaze;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelBlaze.class)
public abstract class MixinModelBlazeCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer blazeHead;
    @Shadow
    private ModelRenderer[] blazeSticks;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", blazeHead);
        parts.put("stick1", blazeSticks[0]);
        parts.put("stick2", blazeSticks[1]);
        parts.put("stick3", blazeSticks[2]);
        parts.put("stick4", blazeSticks[3]);
        parts.put("stick5", blazeSticks[4]);
        parts.put("stick6", blazeSticks[5]);
        parts.put("stick7", blazeSticks[6]);
        parts.put("stick8", blazeSticks[7]);
        parts.put("stick9", blazeSticks[8]);
        parts.put("stick10", blazeSticks[9]);
        parts.put("stick11", blazeSticks[10]);
        parts.put("stick12", blazeSticks[11]);
        return parts;
    }
}
