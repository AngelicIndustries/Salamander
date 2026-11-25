package industries.angelic.salamander.cem.client;

import net.minecraft.client.model.ModelRenderer;

import com.gtnewhorizons.angelica.api.tesr.ModelPartBatch;
import com.gtnewhorizons.angelica.api.tesr.ModelPartMeshBuilder;
import com.gtnewhorizons.angelica.glsm.GLSMDraws;
import com.gtnewhorizons.angelica.rendering.tesr.BatchEligibility;
import com.gtnewhorizons.angelica.rendering.tesr.ModelPartBatcher;

/**
 * Hands pack model parts to Angelica's entity batcher as vanilla parts are handed to it. A part it cannot take draws
 * at once and is reported as Angelica's own fallbacks are, so a renderer drawing pack models stays as eligible for
 * batching as one drawing vanilla parts. Kept separate so standalone clients never resolve Angelica classes.
 */
final class CemAngelicaBatch {

    private static final ModelPartMeshBuilder FACES = (tessellator, part, scale) -> CemGeometry
        .tessellate(tessellator, ((CemProxyPart) part).faces, scale);

    private CemAngelicaBatch() {}

    /** Whether the renderer drawing now batches its parts. */
    static boolean allowed() {
        return BatchEligibility.batchingAllowed() && ModelPartBatcher.INSTANCE.isActive();
    }

    /** Queues a vanilla part's own boxes; false when Angelica cannot take it. */
    static boolean queue(ModelRenderer part, float scale) {
        return ModelPartBatch.partDraw(part, scale);
    }

    /** Queues a pack model part's faces; false when Angelica cannot take them. */
    static boolean queue(CemProxyPart part, float scale) {
        return ModelPartBatch.partDraw(part, scale, FACES);
    }

    static long draws() {
        return GLSMDraws.drawCalls;
    }

    /** Reports what was drawn at once since {@code before}, as a part fallback. */
    static void fallback(long before) {
        BatchEligibility.onPartFallback(before, GLSMDraws.drawCalls);
    }
}
