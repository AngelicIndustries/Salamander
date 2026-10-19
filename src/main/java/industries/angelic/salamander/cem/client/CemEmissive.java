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
        boolean angelica = Loader.isModLoaded("angelica");
        Object condition = angelica ? CemBedShaders.beginEyes() : null;
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDepthMask(false);
            GL11.glColor4f(1, 1, 1, 1);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(mask);
            geometry.run();
        } finally {
            if (angelica) CemBedShaders.endEyes(condition);
            state.restore();
            CemRuntime.texture(texture);
        }
    }
}
