package industries.angelic.salamander.mixins.early.minecraft.client;

import java.util.Map;

import net.minecraft.entity.projectile.EntityArrow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemEntityInputs;

@Mixin(EntityArrow.class)
public abstract class MixinEntityArrowCemInputs implements CemEntityInputs {

    @Shadow
    private boolean inGround;

    @Override
    public void salamander$cemInputs(Map<String, Double> values, float partial) {
        values.put("is_in_ground", inGround ? 1d : 0d);
    }
}
