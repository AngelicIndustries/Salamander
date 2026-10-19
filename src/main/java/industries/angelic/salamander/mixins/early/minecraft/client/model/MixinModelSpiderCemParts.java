package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSpider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelSpider.class)
public abstract class MixinModelSpiderCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer spiderHead;
    @Shadow
    private ModelRenderer spiderNeck;
    @Shadow
    private ModelRenderer spiderBody;
    @Shadow
    private ModelRenderer spiderLeg1;
    @Shadow
    private ModelRenderer spiderLeg2;
    @Shadow
    private ModelRenderer spiderLeg3;
    @Shadow
    private ModelRenderer spiderLeg4;
    @Shadow
    private ModelRenderer spiderLeg5;
    @Shadow
    private ModelRenderer spiderLeg6;
    @Shadow
    private ModelRenderer spiderLeg7;
    @Shadow
    private ModelRenderer spiderLeg8;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", spiderHead);
        parts.put("neck", spiderNeck);
        parts.put("body", spiderBody);
        parts.put("leg1", spiderLeg1);
        parts.put("leg2", spiderLeg2);
        parts.put("leg3", spiderLeg3);
        parts.put("leg4", spiderLeg4);
        parts.put("leg5", spiderLeg5);
        parts.put("leg6", spiderLeg6);
        parts.put("leg7", spiderLeg7);
        parts.put("leg8", spiderLeg8);
        return parts;
    }
}
