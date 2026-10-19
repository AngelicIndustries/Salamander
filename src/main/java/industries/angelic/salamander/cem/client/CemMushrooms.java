package industries.angelic.salamander.cem.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/** OptiFine's dedicated mooshroom mushroom textures, including transparent replacement masks. */
public final class CemMushrooms {

    private CemMushrooms() {}

    public static boolean render(Block block) {
        if (CemRuntime.active() == null || !"mooshroom".equals(CemTargets.target(CemRuntime.subject()))
            || block != Blocks.red_mushroom && block != Blocks.brown_mushroom) return false;
        ResourceLocation texture = new ResourceLocation(
            "textures/entity/cow/" + (block == Blocks.brown_mushroom ? "brown" : "red") + "_mushroom.png");
        if (!CemResources.INSTANCE.exists(texture)) return false;
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        ResourceLocation previousResource = CemRuntime.texture();
        try {
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(texture);
            draw();
            CemEmissive.render(texture, CemMushrooms::draw);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
            CemRuntime.texture(previousResource);
        }
        return true;
    }

    private static void draw() {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setNormal(0, -1, 0);
        for (int diagonal : new int[] { -1, 1 }) for (int side : new int[] { -1, 1 }) {
            double x = .45 * side, z = x * diagonal;
            tessellator.addVertexWithUV(-x, .5, -z, 0, 0);
            tessellator.addVertexWithUV(-x, -.5, -z, 0, 1);
            tessellator.addVertexWithUV(x, -.5, z, 1, 1);
            tessellator.addVertexWithUV(x, .5, z, 1, 0);
        }
        tessellator.draw();
    }
}
