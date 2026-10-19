package industries.angelic.salamander.cem.client;

import java.awt.image.BufferedImage;

/** Original pixels for a downloaded or dynamic player skin. A changed image identity invalidates its CEM atlas. */
public interface CemSkinTexture {

    BufferedImage salamander$skinImage();
}
