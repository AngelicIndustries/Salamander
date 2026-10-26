package industries.angelic.salamander.cem.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;

/** Optional depth-only surface for the old boat renderer, which has no native water-exclusion model. */
public final class CemBoatPatch extends ModelBase implements CemModelParts {

    private static final CemBoatPatch INSTANCE = new CemBoatPatch();
    private final ModelRenderer patch;

    private CemBoatPatch() {
        textureWidth = 128;
        textureHeight = 64;
        patch = new ModelRenderer(this).addBox(-12, -8, -3, 24, 16, 3);
        patch.setRotationPoint(0, 4, 0);
        patch.rotateAngleX = (float) Math.PI / 2;
    }

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        return Collections.singletonMap("water_patch", patch);
    }

    public static void render(Entity entity, float scale) {
        if (!industries.angelic.salamander.config.CemConfig.enabled || !CemResources.INSTANCE.hasModels()) return;
        List<String> targets = Arrays.asList("oak_boat_patch", "boat_patch");
        if (!CemResources.INSTANCE.hasTargets(targets)) return;
        CemRuntime.Draw previous = CemRuntime.beginNamed(INSTANCE, entity, 0, 0, entity.ticksExisted, 0, 0, targets);
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            GL11.glColorMask(false, false, false, false);
            GL11.glDepthMask(true);
            if (CemRuntime.drawing(INSTANCE)) INSTANCE.patch.render(scale);
        } finally {
            try {
                CemRuntime.end(previous, scale);
            } finally {
                GL11.glPopAttrib();
            }
        }
    }
}
