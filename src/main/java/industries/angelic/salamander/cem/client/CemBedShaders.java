package industries.angelic.salamander.cem.client;

import net.coderbot.iris.layer.GbufferPrograms;

/** Kept separate so standalone clients never resolve Angelica classes. */
final class CemBedShaders {

    private CemBedShaders() {}

    /**
     * Picks the shader program for the state just set. While Angelica still batches a renderer's model parts it holds
     * program changes until the renderer ends, so a pass drawn at once in between, such as an emissive one on an
     * armor stand's armor, would draw with the program from before it.
     */
    static void settle() {
        if (!settles) return;
        try {
            com.gtnewhorizons.angelica.glsm.hooks.GLSMHooks.resolvePendingProgram();
        } catch (LinkageError error) {
            // Before Angelica 2.2.15 nothing holds program changes, so there is nothing to settle.
            settles = false;
        }
    }

    private static boolean settles = true;

    static void begin() {
        GbufferPrograms.beginBlockEntities();
    }

    static void end() {
        GbufferPrograms.endBlockEntities();
    }
}
