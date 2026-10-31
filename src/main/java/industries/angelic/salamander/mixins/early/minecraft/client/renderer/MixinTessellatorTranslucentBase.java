package industries.angelic.salamander.mixins.early.minecraft.client.renderer;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.skin.translucent.TranslucentLayers;

/** Splits the semi-transparent texels off a see-through player's base, for renderers that draw it immediately. */
@Mixin(Tessellator.class)
public abstract class MixinTessellatorTranslucentBase {

    @WrapMethod(method = "draw")
    private int salamander$splitSeeThroughBase(Operation<Integer> original) {
        if (!TranslucentLayers.splitsBase()) return original.call();
        return TranslucentLayers.splitBase((AccessorTessellator) this, original::call);
    }
}
