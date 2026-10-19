package industries.angelic.salamander.mixins.late.etfuturum;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import industries.angelic.salamander.cem.client.CemEntityInputs;

@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.entities.EntityBee", remap = false)
public abstract class MixinEntityBeeCemInputs implements CemEntityInputs {

    @Shadow
    private int getAnger() {
        throw new AssertionError();
    }

    @Unique
    private int salamander$angerStart;

    @Override
    public void salamander$cemInputs(Map<String, Double> inputs, float partial) {
        int anger = getAnger();
        salamander$angerStart = anger == 0 ? 0 : Math.max(salamander$angerStart, anger);
        inputs.put("is_aggressive", anger > 0 ? 1d : 0d);
        inputs.put("anger_time", Math.max(0, anger - partial) * 1d);
        inputs.put("anger_time_start", (double) salamander$angerStart);
    }
}
