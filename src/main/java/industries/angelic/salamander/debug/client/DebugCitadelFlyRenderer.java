package industries.angelic.salamander.debug.client;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import industries.angelic.salamander.Salamander;

public final class DebugCitadelFlyRenderer extends RenderLiving {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        Salamander.MODID,
        "textures/debug/citadel_fly.png");

    public DebugCitadelFlyRenderer() {
        super(new DebugCitadelFlyModel(), 0.35F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }
}
