package industries.angelic.salamander.mixins.late.armoredarms;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.compat.armoredarms.ArmoredArmsCompat;

/** A CEM player pack's hand replaces ArmoredArms' own arm; ArmoredArms' armor layers still draw over it. */
@Pseudo
@Mixin(targets = "com.artur114.armoredarms.client.modelrender.player.ArmModelRendererPlayer", remap = false)
public abstract class MixinArmModelRendererPlayerCem {

    @Inject(
        method = "renderArm(Lcom/artur114/armoredarms/client/modelrender/player/ArmModelManagerPlayer;Lcom/artur114/armoredarms/core/api/EnumHandSideAA;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void salamander$cemHand(@Coerce Object manager, @Coerce Object side, CallbackInfo ci) {
        if (ArmoredArmsCompat.cemArm(manager, side)) ci.cancel();
    }
}
