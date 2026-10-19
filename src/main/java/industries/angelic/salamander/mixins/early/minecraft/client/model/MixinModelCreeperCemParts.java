package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelCreeper;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelCreeper.class)
public abstract class MixinModelCreeperCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer head;
    @Shadow
    private ModelRenderer body;
    @Shadow
    private ModelRenderer leg1;
    @Shadow
    private ModelRenderer leg2;
    @Shadow
    private ModelRenderer leg3;
    @Shadow
    private ModelRenderer leg4;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", head);
        parts.put("body", body);
        parts.put("leg1", leg1);
        parts.put("leg2", leg2);
        parts.put("leg3", leg3);
        parts.put("leg4", leg4);
        return parts;
    }
}
