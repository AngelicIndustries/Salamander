package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.RenderEnderman;
import net.minecraft.entity.monster.EntityEnderman;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(RenderEnderman.class)
public abstract class MixinRenderEndermanCem {

    /** As EMF's {@code enderman_block}, the attachment moves the carried block on from where vanilla holds it. */
    @Inject(
        method = "renderEquippedItems(Lnet/minecraft/entity/monster/EntityEnderman;F)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 0, remap = false))
    private void salamander$carriedBlock(EntityEnderman enderman, float partial, CallbackInfo ci) {
        CemRuntime.attach("enderman_block", .0625f);
    }
}
