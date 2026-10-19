package industries.angelic.salamander.mixins.late.etfuturum;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemEntityInputs;

@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.entities.EntityFox", remap = false)
public abstract class MixinEntityFoxCemInputs implements CemEntityInputs {

    @Shadow
    public abstract boolean isSitting();

    @Shadow
    public abstract boolean isInSneakingPose();

    @Override
    public void salamander$cemInputs(Map<String, Double> inputs, float partial) {
        inputs.put("is_sitting", isSitting() ? 1d : 0d);
        inputs.put("is_sneaking", isInSneakingPose() ? 1d : 0d);
    }
}
