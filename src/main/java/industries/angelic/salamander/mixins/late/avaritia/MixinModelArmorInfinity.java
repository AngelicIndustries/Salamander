package industries.angelic.salamander.mixins.late.avaritia;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.compat.avaritia.AvaritiaOverlay;

/** Each time Avaritia readies its infinity armor for a wearer, the full-set shimmer's arms are fitted to theirs. */
@Pseudo
@Mixin(targets = "fox.spiteful.avaritia.render.ModelArmorInfinity", remap = false)
public abstract class MixinModelArmorInfinity {

    @Inject(method = "update", at = @At("TAIL"), remap = false, require = 1)
    private void salamander$fitShimmer(EntityLivingBase wearer, ItemStack stack, int slot, CallbackInfo ci) {
        AvaritiaOverlay.fit((ModelBiped) (Object) this, wearer);
    }
}
