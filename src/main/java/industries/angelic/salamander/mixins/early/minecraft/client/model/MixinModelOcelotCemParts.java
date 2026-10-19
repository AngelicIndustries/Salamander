package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelOcelot;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelOcelot.class)
public abstract class MixinModelOcelotCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer ocelotHead;
    @Shadow
    private ModelRenderer ocelotBody;
    @Shadow
    private ModelRenderer ocelotTail;
    @Shadow
    private ModelRenderer ocelotTail2;
    @Shadow
    private ModelRenderer ocelotBackLeftLeg;
    @Shadow
    private ModelRenderer ocelotBackRightLeg;
    @Shadow
    private ModelRenderer ocelotFrontLeftLeg;
    @Shadow
    private ModelRenderer ocelotFrontRightLeg;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", ocelotHead);
        parts.put("body", ocelotBody);
        parts.put("tail", ocelotTail);
        parts.put("tail2", ocelotTail2);
        parts.put("back_left_leg", ocelotBackLeftLeg);
        parts.put("back_right_leg", ocelotBackRightLeg);
        parts.put("front_left_leg", ocelotFrontLeftLeg);
        parts.put("front_right_leg", ocelotFrontRightLeg);
        return parts;
    }
}
