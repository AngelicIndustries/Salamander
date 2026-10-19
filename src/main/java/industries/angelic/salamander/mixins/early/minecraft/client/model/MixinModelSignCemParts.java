package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSign;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelSign.class)
public abstract class MixinModelSignCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer signBoard;
    @Shadow
    private ModelRenderer signStick;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("board", signBoard);
        parts.put("stick", signStick);
        return parts;
    }
}
