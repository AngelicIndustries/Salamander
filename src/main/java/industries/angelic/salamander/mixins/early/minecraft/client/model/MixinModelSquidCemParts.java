package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSquid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelSquid.class)
public abstract class MixinModelSquidCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer squidBody;
    @Shadow
    private ModelRenderer[] squidTentacles;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("body", squidBody);
        parts.put("tentacle1", squidTentacles[0]);
        parts.put("tentacle2", squidTentacles[1]);
        parts.put("tentacle3", squidTentacles[2]);
        parts.put("tentacle4", squidTentacles[3]);
        parts.put("tentacle5", squidTentacles[4]);
        parts.put("tentacle6", squidTentacles[5]);
        parts.put("tentacle7", squidTentacles[6]);
        parts.put("tentacle8", squidTentacles[7]);
        return parts;
    }
}
