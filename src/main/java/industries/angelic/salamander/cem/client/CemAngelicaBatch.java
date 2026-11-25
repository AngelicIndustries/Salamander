package industries.angelic.salamander.cem.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import net.minecraft.client.model.ModelRenderer;

import com.gtnewhorizons.angelica.api.tesr.ModelPartBatch;
import com.gtnewhorizons.angelica.api.tesr.ModelPartMeshBuilder;
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

    /** Angelica's count of draw calls: in GLSMDraws from 2.2.40, in GLStateManager before. */
    private static final MethodHandle DRAWS = drawCounter();

    private CemAngelicaBatch() {}

    private static MethodHandle drawCounter() {
        for (String owner : new String[] { "com.gtnewhorizons.angelica.glsm.GLSMDraws",
            "com.gtnewhorizons.angelica.glsm.GLStateManager" }) {
            try {
                return MethodHandles.publicLookup()
                    .findStaticGetter(Class.forName(owner), "drawCalls", long.class);
            } catch (ReflectiveOperationException | LinkageError exception) {
                // Not in this Angelica: try the older place.
            }
        }
        return null;
    }

    /**
     * Whether this Angelica has everything used here. Its batcher API came in 2.2.15; anything older, or a later
     * change to these methods, leaves pack model parts drawn at once as before.
     */
    static boolean available() {
        try {
            ModelPartBatch.class.getMethod("partDraw", ModelRenderer.class, float.class, ModelPartMeshBuilder.class);
            ModelPartBatch.class.getMethod("partDraw", ModelRenderer.class, float.class);
            BatchEligibility.class.getMethod("onPartFallback", long.class, long.class);
            BatchEligibility.class.getMethod("batchingAllowed");
            ModelPartBatcher.class.getMethod("isActive");
            return DRAWS != null;
        } catch (NoSuchMethodException | LinkageError exception) {
            return false;
        }
    }

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
        try {
            return (long) DRAWS.invokeExact();
        } catch (Throwable exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** Reports what was drawn at once since {@code before}, as a part fallback. */
    static void fallback(long before) {
        BatchEligibility.onPartFallback(before, draws());
    }
}
