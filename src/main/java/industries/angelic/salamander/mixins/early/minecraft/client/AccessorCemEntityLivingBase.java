package industries.angelic.salamander.mixins.early.minecraft.client;

import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLivingBase.class)
public interface AccessorCemEntityLivingBase {

    /** Whether the entity holds jump. On the client only the local player sets it, as in EMF. */
    @Accessor("isJumping")
    boolean salamander$isJumping();
}
