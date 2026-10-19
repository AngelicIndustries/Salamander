package industries.angelic.salamander.mixins.early.minecraft.client;

import java.util.Map;

import net.minecraft.entity.monster.EntitySilverfish;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.client.CemEntityInputs;

@Mixin(EntitySilverfish.class)
public abstract class MixinEntitySilverfishCemInputs implements CemEntityInputs {

    @Unique
    private float salamander$previousBodyYaw;
    @Unique
    private boolean salamander$hasBodyYaw;

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void salamander$captureBodyYaw(CallbackInfo ci) {
        // Silverfish overwrite renderYawOffset before super.onUpdate records prevRenderYawOffset.
        // Preserve the actual last tick's body orientation for interpolation in CEM render scopes.
        salamander$previousBodyYaw = ((EntitySilverfish) (Object) this).renderYawOffset;
        salamander$hasBodyYaw = true;
    }

    @Override
    public float salamander$cemPreviousBodyYaw(float original) {
        return salamander$hasBodyYaw ? salamander$previousBodyYaw : original;
    }

    @Override
    public void salamander$cemInputs(Map<String, Double> inputs, float partialTicks) {}
}
