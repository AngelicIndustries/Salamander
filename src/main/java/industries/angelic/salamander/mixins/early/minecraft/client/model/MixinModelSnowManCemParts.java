package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSnowMan;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelSnowMan.class)
public abstract class MixinModelSnowManCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer head;
    @Shadow
    private ModelRenderer body;
    @Shadow
    private ModelRenderer bottomBody;
    @Shadow
    private ModelRenderer rightHand;
    @Shadow
    private ModelRenderer leftHand;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", head);
        parts.put("body", body);
        parts.put("body_bottom", bottomBody);
        parts.put("right_hand", rightHand);
        parts.put("left_hand", leftHand);
        return parts;
    }
}
