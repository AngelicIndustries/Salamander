package industries.angelic.salamander.cem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/** Native-textured fallback pieces in the standard chest-boat coordinate system. */
final class CemBoatChest {

    private static final ModelChest MODEL = new ModelChest();
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/chest/normal.png");

    private CemBoatChest() {}

    static boolean part(String name) {
        return "chest_base".equals(name) || "chest_lid".equals(name) || "chest_knob".equals(name);
    }

    static void render(String name, float scale) {
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        ResourceLocation previous = CemRuntime.texture();
        GL11.glPushMatrix();
        try {
            ModelRenderer part;
            if (name.equals("chest_base")) {
                part = MODEL.chestBelow;
                GL11.glScalef(12f / 14f, .8f, 12f / 14f);
            } else if (name.equals("chest_lid")) {
                part = MODEL.chestLid;
                GL11.glScalef(12f / 14f, 1, 12f / 14f);
                GL11.glTranslatef(0, 5 * scale, 14 * scale);
            } else {
                part = MODEL.chestKnob;
                GL11.glTranslatef(scale, 2 * scale, 15 * scale);
            }
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(TEXTURE);
            Runnable geometry = () -> { for (ModelBox box : part.cubeList) box.render(Tessellator.instance, scale); };
            geometry.run();
            CemEmissive.render(TEXTURE, geometry);
        } finally {
            GL11.glPopMatrix();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            CemRuntime.texture(previous);
        }
    }
}
