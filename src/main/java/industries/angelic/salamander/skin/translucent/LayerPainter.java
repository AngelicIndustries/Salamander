package industries.angelic.salamander.skin.translucent;

import industries.angelic.salamander.api.skin.LayerSegment;

/** Paints one layer segment under the current GL state, flat or as voxels. */
public interface LayerPainter {

    void paint(LayerSegment segment, boolean volumetric);
}
