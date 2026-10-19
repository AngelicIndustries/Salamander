package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelWither;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelWither.class)
public abstract class MixinModelWitherCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer[] field_82905_a;
    @Shadow
    private ModelRenderer[] field_82904_b;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("body1", field_82905_a[0]);
        parts.put("body2", field_82905_a[1]);
        parts.put("body3", field_82905_a[2]);
        parts.put("head1", field_82904_b[0]);
        parts.put("head2", field_82904_b[1]);
        parts.put("head3", field_82904_b[2]);
        return parts;
    }
}
