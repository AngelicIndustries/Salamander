package industries.angelic.salamander.mixins.late.etfuturum;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemModelParts;
import industries.angelic.salamander.cem.client.CemRuntime;

@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.client.model.ModelBanner", remap = false)
public abstract class MixinModelBannerCem implements CemModelParts {

    @Shadow
    private ModelRenderer bannerSlate;
    @Shadow
    private ModelRenderer bannerStand;
    @Shadow
    private ModelRenderer bannerTop;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("slate", bannerSlate);
        parts.put("stand", bannerStand);
        parts.put("top", bannerTop);
        return parts;
    }

    @WrapMethod(method = "renderAll")
    private void salamander$render(Operation<Void> original) {
        CemRuntime.Draw previous = CemRuntime.begin((ModelBase) (Object) this, null, 0, 0, 0, 0, 0);
        try {
            original.call();
        } finally {
            CemRuntime.end(previous, 0.0625F);
        }
    }
}
