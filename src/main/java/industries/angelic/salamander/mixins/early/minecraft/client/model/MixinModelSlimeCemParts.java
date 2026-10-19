package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSlime;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelSlime.class)
public abstract class MixinModelSlimeCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer slimeBodies;
    @Shadow
    private ModelRenderer slimeRightEye;
    @Shadow
    private ModelRenderer slimeLeftEye;
    @Shadow
    private ModelRenderer slimeMouth;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("body", slimeBodies);
        parts.put("right_eye", slimeRightEye);
        parts.put("left_eye", slimeLeftEye);
        parts.put("mouth", slimeMouth);
        return parts;
    }
}
