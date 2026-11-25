package industries.angelic.salamander.cem.client;

import net.coderbot.iris.layer.GbufferPrograms;

/** Kept separate so standalone clients never resolve Angelica classes. */
final class CemBedShaders {

    private CemBedShaders() {}

    static Object beginEyes() {
        Object previous = GbufferPrograms.getSpecialCondition();
        GbufferPrograms
            .setupSpecialRenderCondition(net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition.ENTITY_EYES);
        return previous;
    }

    static void endEyes(Object previous) {
        if (previous == null) GbufferPrograms.teardownSpecialRenderCondition();
        else GbufferPrograms
            .setupSpecialRenderCondition((net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition) previous);
    }

    /**
     * Picks the shader program for the state just set. While Angelica still batches a renderer's model parts it holds
     * program changes until the renderer ends, so a pass drawn at once in between, such as an emissive one on an
     * armor stand's armor, would draw with the program from before it.
     */
    static void settle() {
        com.gtnewhorizons.angelica.glsm.hooks.GLSMHooks.resolvePendingProgram();
    }

    static void begin() {
        GbufferPrograms.beginBlockEntities();
    }

    static void end() {
        GbufferPrograms.endBlockEntities();
    }
}
