package industries.angelic.salamander.cem.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import industries.angelic.salamander.api.skin.LayerDraw;
import industries.angelic.salamander.api.skin.LayerSegment;
import industries.angelic.salamander.api.skin.SkinLayers;
import industries.angelic.salamander.api.skin.SkinPart;

/** A CEM player pack's overlay parts, drawn by Salamander's skin layers on the pack's bones. */
public final class CemLayers {

    /** CEM part names of the overlay parts. */
    private static final Map<String, SkinPart> SKIN_PARTS = new HashMap<>();

    static {
        SKIN_PARTS.put("headwear", SkinPart.HAT);
        SKIN_PARTS.put("jacket", SkinPart.JACKET);
        SKIN_PARTS.put("left_sleeve", SkinPart.LEFT_SLEEVE);
        SKIN_PARTS.put("right_sleeve", SkinPart.RIGHT_SLEEVE);
        SKIN_PARTS.put("left_pants", SkinPart.LEFT_PANTS);
        SKIN_PARTS.put("right_pants", SkinPart.RIGHT_PANTS);
    }

    private CemLayers() {}

    /**
     * Takes over the given overlay parts (CEM names, such as {@code jacket}) in the innermost open layer draw, the one
     * around the model being replaced. The pack draws those parts itself, handing vanilla-shaped pieces back through
     * {@link #submit}; the layers keep drawing the others on the pack's bones.
     */
    public static void claim(Collection<String> cemParts) {
        LayerDraw draw = SkinLayers.current();
        if (draw == null) return;
        for (String cemPart : cemParts) {
            SkinPart part = SKIN_PARTS.get(cemPart);
            if (part != null) draw.claim(part);
        }
    }

    /**
     * Draws a vanilla overlay piece under the current transform, flat or 3D as the viewer chose, if the open layer draw
     * has its part claimed. False when the caller must draw the box itself.
     */
    public static boolean submit(CemOverlays.Overlay overlay) {
        LayerDraw draw = SkinLayers.current();
        SkinPart part = SkinPart.valueOf(overlay.part);
        if (draw == null || !draw.claimed(part)) return false;
        if (overlay.segment == null) {
            LayerSegment segment = LayerSegment.box(
                part,
                overlay.x,
                overlay.y,
                overlay.z,
                overlay.width,
                overlay.height,
                overlay.depth,
                overlay.u,
                overlay.v);
            overlay.segment = overlay.whole() ? segment : segment.rows(overlay.rowStart, overlay.rowEnd);
        }
        draw.submit(overlay.segment);
        return true;
    }
}
