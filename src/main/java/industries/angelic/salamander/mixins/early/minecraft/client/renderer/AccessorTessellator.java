package industries.angelic.salamander.mixins.early.minecraft.client.renderer;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A draw's vertices, for see-through skins to paint again later. */
@Mixin(Tessellator.class)
public interface AccessorTessellator {

    @Accessor("rawBuffer")
    int[] salamander$getRawBuffer();

    @Accessor("vertexCount")
    int salamander$getVertexCount();

    @Accessor("drawMode")
    int salamander$getDrawMode();

    @Accessor("hasTexture")
    boolean salamander$hasTexture();

    @Accessor("hasBrightness")
    boolean salamander$hasBrightness();

    @Accessor("hasColor")
    boolean salamander$hasColor();

    @Accessor("hasNormals")
    boolean salamander$hasNormals();
}
