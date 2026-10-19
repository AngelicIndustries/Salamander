package industries.angelic.salamander.mixins.late.etfuturum;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemModelParts;
import industries.angelic.salamander.cem.client.CemRuntime;

@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.client.model.ModelHead", remap = false)
public abstract class MixinModelHeadCem implements CemModelParts {

    @Shadow
    @Final
    private ModelRenderer head;
    @Shadow
    @Final
    private ModelRenderer overlay;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", head);
        parts.put("$group:head:overlay", overlay);
        return parts;
    }

    @WrapMethod(method = "render(FF)V")
    private void salamander$render(float pitch, float yaw, Operation<Void> original) {
        CemRuntime.Draw previous = CemRuntime.begin((ModelBase) (Object) this, null, 0, 0, 0, yaw, pitch);
        try {
            original.call(pitch, yaw);
        } finally {
            CemRuntime.end(previous, 0.0625F);
        }
    }
}
