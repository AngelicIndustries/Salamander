package industries.angelic.salamander.mixins.early.minecraft.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelSign;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(ModelSign.class)
public abstract class MixinModelSignCemRender {

    @WrapMethod(method = "renderSign")
    private void salamander$render(Operation<Void> original) {
        CemRuntime.Draw previous = CemRuntime.begin((ModelBase) (Object) this, null, 0, 0, 0, 0, 0);
        try {
            original.call();
        } finally {
            CemRuntime.end(previous, 0.0625F);
        }
    }
}
