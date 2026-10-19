package industries.angelic.salamander.mixins.early.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.client.CemMushrooms;

@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocksCemMushrooms {

    @Inject(method = "renderBlockAsItem", at = @At("HEAD"), cancellable = true)
    private void salamander$mushrooms(Block block, int meta, float brightness, CallbackInfo ci) {
        if (CemMushrooms.render(block)) ci.cancel();
    }
}
