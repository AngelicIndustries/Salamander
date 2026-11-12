package industries.angelic.salamander.mixins.early.minecraft.client.renderer;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.skin.translucent.TranslucentLayers;

/** Splits the semi-transparent texels off a see-through player's base, for renderers that draw it immediately. */
@Mixin(Tessellator.class)
public abstract class MixinTessellatorTranslucentBase {

    /** No arguments, shared: every draw passes here, and the usual case shouldn't allocate them. */
    @Unique
    private static final Object[] salamander$NO_ARGUMENTS = new Object[0];

    @WrapMethod(method = "draw")
    private int salamander$splitSeeThroughBase(Operation<Integer> original) {
        if (!TranslucentLayers.splitsBase()) return original.call(salamander$NO_ARGUMENTS);
        return TranslucentLayers.splitBase((AccessorTessellator) this, original::call);
    }
}
