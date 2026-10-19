package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelWolf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelWolf.class)
public abstract class MixinModelWolfCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer wolfHeadMain;
    @Shadow
    private ModelRenderer wolfBody;
    @Shadow
    private ModelRenderer wolfTail;
    @Shadow
    private ModelRenderer wolfMane;
    @Shadow
    private ModelRenderer wolfLeg1;
    @Shadow
    private ModelRenderer wolfLeg2;
    @Shadow
    private ModelRenderer wolfLeg3;
    @Shadow
    private ModelRenderer wolfLeg4;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", wolfHeadMain);
        parts.put("body", wolfBody);
        parts.put("tail", wolfTail);
        parts.put("mane", wolfMane);
        parts.put("leg1", wolfLeg1);
        parts.put("leg2", wolfLeg2);
        parts.put("leg3", wolfLeg3);
        parts.put("leg4", wolfLeg4);
        return parts;
    }
}
