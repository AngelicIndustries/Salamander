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

    static void begin() {
        GbufferPrograms.beginBlockEntities();
    }

    static void end() {
        GbufferPrograms.endBlockEntities();
    }
}
