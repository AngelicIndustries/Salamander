package industries.angelic.salamander.mixins.early.minecraft.client;

import java.util.Map;

import net.minecraft.entity.passive.EntitySquid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.animation.CemWalkAnimation;
import industries.angelic.salamander.cem.client.CemEntityInputs;

@Mixin(EntitySquid.class)
public abstract class MixinEntitySquidCemInputs implements CemEntityInputs {

    @Unique
    private final CemWalkAnimation salamander$swimming = new CemWalkAnimation();

    @Inject(method = "onLivingUpdate", at = @At("RETURN"))
    private void salamander$motion(CallbackInfo ci) {
        EntitySquid squid = (EntitySquid) (Object) this;
        if (squid.worldObj.isRemote) salamander$swimming.tick(squid.posX - squid.prevPosX, squid.posZ - squid.prevPosZ);
    }

    @Override
    public void salamander$cemInputs(Map<String, Double> values, float partialTicks) {
        // Squid bypass the vanilla walk update. A hurt packet sets limbSwingAmount to 1.5 permanently,
        // while prevLimbSwingAmount stays zero, producing a sawtooth every tick if interpolated directly.
        values.put("limb_speed", salamander$swimming.speed(partialTicks));
        values.put("limb_swing", salamander$swimming.position(partialTicks));
    }
}
