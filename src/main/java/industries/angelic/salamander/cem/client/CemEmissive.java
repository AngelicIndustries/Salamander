package industries.angelic.salamander.cem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.geckolib.renderer.base.GlStateSnapshot;

import cpw.mods.fml.common.Loader;

/** Balanced full-bright draw for a CEM texture's optional emissive mask. */
final class CemEmissive {

    private CemEmissive() {}

    static boolean eyes(ResourceLocation texture) {
        return texture != null && texture.getResourcePath()
            .contains("eyes");
    }

    static void render(ResourceLocation texture, Runnable geometry) {
        ResourceLocation mask = CemResources.INSTANCE.emissive(texture);
        if (mask == null) return;
        Object subject = CemRuntime.subject();
        // These renderers already provide an eye pass, including their shader hooks.
        if (subject instanceof net.minecraft.entity.monster.EntitySpider
            || subject instanceof net.minecraft.entity.monster.EntityEnderman
            || subject instanceof net.minecraft.entity.boss.EntityDragon) return;
        if (subject instanceof net.minecraft.entity.Entity && ((net.minecraft.entity.Entity) subject).isInvisible())
            return;
        GlStateSnapshot state = GlStateSnapshot.capture();
        boolean offset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
        float factor = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR),
            units = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS);
        boolean angelica = Loader.isModLoaded("angelica"), batching = CemGeometry.batching();
        try {
            // As ETF draws a mask by default: the model's cutout layer again, at full light. A shader pack draws it
            // as entity geometry too, opaque where the mask is, rather than as glowing eyes it may blend additively.
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            // The same faces again: pulled toward the camera, so they never lose to the base pass, whatever jitter a
            // shader pack's anti-aliasing adds.
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-1, -1);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glColor4f(1, 1, 1, 1);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(mask);
            // Parts queued with Angelica's batcher take the state along; parts drawn at once need its program now.
            if (angelica && !batching) CemBedShaders.settle();
            geometry.run();
        } finally {
            state.restore();
            GL11.glPolygonOffset(factor, units);
            if (!offset) GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            if (angelica && !batching) CemBedShaders.settle();
            CemGeometry.restoreTexture(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D), texture);
        }
    }
}
