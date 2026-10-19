package industries.angelic.salamander.mixins.late.etfuturum;

import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.client.CemRuntime;

@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.client.renderer.entity.ChestBoatRenderer", remap = false)
public abstract class MixinChestBoatRendererCem {

    @Inject(method = "renderExtraBoatContents", at = @At("HEAD"), cancellable = true, remap = false)
    private void salamander$chest(@Coerce Entity boat, float partialTicks, CallbackInfo ci) {
        if (CemRuntime.active() != null && CemRuntime.active().model.originalParts.containsKey("chest_base"))
            ci.cancel();
    }
}
