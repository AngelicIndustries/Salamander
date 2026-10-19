package industries.angelic.salamander.debug.client;

import java.io.IOException;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.github.alexthe666.citadel.client.model.TabulaModel;
import com.github.alexthe666.citadel.client.model.TabulaModelHandler;

import industries.angelic.salamander.Salamander;

/** Renders Ice and Fire's actual 1.18 fire-dragon Tabula model. */
public final class DebugCitadelDragonRenderer extends RenderLiving {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        Salamander.MODID,
        "textures/debug/citadel_fire_dragon.png");

    public DebugCitadelDragonRenderer() {
        super(loadModel(), 1.5F);
    }

    private static TabulaModel loadModel() {
        try {
            return new TabulaModel(
                TabulaModelHandler.INSTANCE.loadTabulaModel("/assets/salamander/models/tabula/fire_dragon"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load the Ice and Fire dragon fixture", exception);
        }
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        GL11.glScalef(1.5F, 1.5F, 1.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }
}
