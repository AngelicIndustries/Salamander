package industries.angelic.salamander.mixins.early.minecraft.client;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.TexturedQuad;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelBox.class)
public interface AccessorCemModelBox {

    /** The box's six faces, inflated, mirrored and UV-mapped as vanilla draws them. */
    @Accessor("quadList")
    TexturedQuad[] salamander$quads();
}
