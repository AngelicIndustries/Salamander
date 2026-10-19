package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelDragon;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelDragon.class)
public abstract class MixinModelDragonCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer head;
    @Shadow
    private ModelRenderer jaw;
    @Shadow
    private ModelRenderer body;
    @Shadow
    private ModelRenderer spine;
    @Shadow
    private ModelRenderer wing;
    @Shadow
    private ModelRenderer wingTip;
    @Shadow
    private ModelRenderer frontLeg;
    @Shadow
    private ModelRenderer frontLegTip;
    @Shadow
    private ModelRenderer frontFoot;
    @Shadow
    private ModelRenderer rearLeg;
    @Shadow
    private ModelRenderer rearLegTip;
    @Shadow
    private ModelRenderer rearFoot;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", head);
        parts.put("jaw", jaw);
        parts.put("body", body);
        parts.put("neck1", spine);
        parts.put("neck2", spine);
        parts.put("neck3", spine);
        parts.put("neck4", spine);
        parts.put("neck5", spine);
        parts.put("tail1", spine);
        parts.put("tail2", spine);
        parts.put("tail3", spine);
        parts.put("tail4", spine);
        parts.put("tail5", spine);
        parts.put("tail6", spine);
        parts.put("tail7", spine);
        parts.put("tail8", spine);
        parts.put("tail9", spine);
        parts.put("tail10", spine);
        parts.put("tail11", spine);
        parts.put("tail12", spine);
        parts.put("left_wing", wing);
        parts.put("left_wing_tip", wingTip);
        parts.put("front_left_leg", frontLeg);
        parts.put("front_left_shin", frontLegTip);
        parts.put("front_left_foot", frontFoot);
        parts.put("back_left_leg", rearLeg);
        parts.put("back_left_shin", rearLegTip);
        parts.put("back_left_foot", rearFoot);
        parts.put("right_wing", wing);
        parts.put("right_wing_tip", wingTip);
        parts.put("front_right_leg", frontLeg);
        parts.put("front_right_shin", frontLegTip);
        parts.put("front_right_foot", frontFoot);
        parts.put("back_right_leg", rearLeg);
        parts.put("back_right_shin", rearLegTip);
        parts.put("back_right_foot", rearFoot);
        return parts;
    }
}
