package industries.angelic.salamander.mixins.late.armoredarms;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.compat.armoredarms.ArmoredArmsSleeves;

/** The sleeve goes on right after ArmoredArms draws the arm, so its armor layers still cover it. */
@Pseudo
@Mixin(targets = "com.artur114.armoredarms.client.modelrender.player.ArmModelRendererPlayer", remap = false)
public abstract class MixinArmModelRendererPlayerSleeve {

    @Inject(
        method = "renderArm(Lcom/artur114/armoredarms/client/modelrender/player/ArmModelManagerPlayer;Lcom/artur114/armoredarms/core/api/EnumHandSideAA;)V",
        at = @At("RETURN"),
        remap = false)
    private void salamander$sleeve(@Coerce Object manager, @Coerce Object side, CallbackInfo ci) {
        ArmoredArmsSleeves.afterArm(manager, side);
    }
}
