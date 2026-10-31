package industries.angelic.salamander.api.skin;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.config.SkinConfig;
import industries.angelic.salamander.config.SkinLayers3DConfig;

/** How this client draws other players' layers. Local settings, never synced. */
@SideOnly(Side.CLIENT)
public final class ViewerSkinSettings {

    private ViewerSkinSettings() {}

    /** Whether this viewer draws the part's layer as voxels where possible. */
    public static boolean volumetric(SkinPart part) {
        if (!SkinLayers3DConfig.enabled3D) return false;
        return switch (part) {
            case HAT -> SkinLayers3DConfig.layers.enableHat3D;
            case JACKET -> SkinLayers3DConfig.layers.enableJacket3D;
            case LEFT_SLEEVE -> SkinLayers3DConfig.layers.enableLeftSleeve3D;
            case RIGHT_SLEEVE -> SkinLayers3DConfig.layers.enableRightSleeve3D;
            case LEFT_PANTS -> SkinLayers3DConfig.layers.enableLeftPants3D;
            case RIGHT_PANTS -> SkinLayers3DConfig.layers.enableRightPants3D;
            case CAPE -> false;
        };
    }

    /** Beyond this many blocks, layers are drawn flat. */
    public static int lodDistance() {
        return SkinLayers3DConfig.renderDistanceLOD;
    }

    /** Whether a layer covered by armor is hidden instead of drawn flat. */
    public static boolean hideOverlayUnderArmor() {
        return SkinConfig.hideOverlayArmor;
    }
}
