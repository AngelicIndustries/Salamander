package industries.angelic.salamander.mixins.early.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.salamander.cem.client.CemBeds;

@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocksCemBed {

    @Inject(method = "renderBlockBed", at = @At("HEAD"), cancellable = true)
    private void salamander$bed(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (CemBeds.track(block, x, y, z)) cir.setReturnValue(true);
    }
}
