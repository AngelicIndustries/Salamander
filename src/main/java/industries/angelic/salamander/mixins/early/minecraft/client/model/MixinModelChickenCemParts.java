package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelChicken;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelChicken.class)
public abstract class MixinModelChickenCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer head;
    @Shadow
    private ModelRenderer body;
    @Shadow
    private ModelRenderer rightLeg;
    @Shadow
    private ModelRenderer leftLeg;
    @Shadow
    private ModelRenderer rightWing;
    @Shadow
    private ModelRenderer leftWing;
    @Shadow
    private ModelRenderer bill;
    @Shadow
    private ModelRenderer chin;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", head);
        parts.put("body", body);
        parts.put("right_leg", rightLeg);
        parts.put("left_leg", leftLeg);
        parts.put("right_wing", rightWing);
        parts.put("left_wing", leftWing);
        parts.put("bill", bill);
        parts.put("chin", chin);
        return parts;
    }
}
