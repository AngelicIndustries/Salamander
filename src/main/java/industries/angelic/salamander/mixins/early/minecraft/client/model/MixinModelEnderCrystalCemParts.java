package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelEnderCrystal;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelEnderCrystal.class)
public abstract class MixinModelEnderCrystalCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer cube;
    @Shadow
    private ModelRenderer glass;
    @Shadow
    private ModelRenderer base;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("cube", cube);
        parts.put("outer_glass", glass);
        parts.put("inner_glass", glass);
        parts.put("base", base);
        return parts;
    }
}
