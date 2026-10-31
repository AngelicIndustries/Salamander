package industries.angelic.salamander.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.salamander.Salamander;

/**
 * The skin's outer layer drawn in 3D on players. Skulls have their own in {@link SkullConfig}.
 * <p>
 * Field defaults match 3d-Skin-Layers upstream where applicable.
 */
@Config(modid = Salamander.MODID, category = "skinlayers3d", configSubDirectory = Salamander.MODID)
@Config.LangKeyPattern(pattern = "%mod.config.%cat.%field")
public final class SkinLayers3DConfig {

    @Config.Comment("Master toggle for all 3D skin layer rendering (players and skulls). Not compatible with SmartMoving.")
    @Config.DefaultBoolean(true)
    public static boolean enabled3D = true;

    @Config.Comment("Distance in blocks before falling back to flat 2D overlays.")
    @Config.DefaultInt(14)
    @Config.RangeInt(min = 1, max = 64)
    public static int renderDistanceLOD = 14;

    @Config.Comment("Which of the skin's outer layers are drawn in 3D.")
    public static final Layers layers = new Layers();

    @Config.Comment("Base voxel size for limbs and body.")
    @Config.DefaultFloat(1.15f)
    @Config.RangeFloat(min = 0.5f, max = 2.0f)
    public static float baseVoxelSize = 1.15f;

    @Config.Comment("Body voxel width scale.")
    @Config.DefaultFloat(1.05f)
    @Config.RangeFloat(min = 0.5f, max = 2.0f)
    public static float bodyVoxelWidthSize = 1.05f;

    @Config.Comment("Head voxel size scale.")
    @Config.DefaultFloat(1.18f)
    @Config.RangeFloat(min = 0.5f, max = 2.0f)
    public static float headVoxelSize = 1.18f;

    private SkinLayers3DConfig() {}

    public static class Layers {

        @Config.Comment("Render hat layer as 3D voxels.")
        @Config.DefaultBoolean(true)
        public boolean enableHat3D = true;

        @Config.Comment("Render jacket layer as 3D voxels.")
        @Config.DefaultBoolean(true)
        public boolean enableJacket3D = true;

        @Config.Comment("Render left sleeve layer as 3D voxels.")
        @Config.DefaultBoolean(true)
        public boolean enableLeftSleeve3D = true;

        @Config.Comment("Render right sleeve layer as 3D voxels.")
        @Config.DefaultBoolean(true)
        public boolean enableRightSleeve3D = true;

        @Config.Comment("Render left pants layer as 3D voxels.")
        @Config.DefaultBoolean(true)
        public boolean enableLeftPants3D = true;

        @Config.Comment("Render right pants layer as 3D voxels.")
        @Config.DefaultBoolean(true)
        public boolean enableRightPants3D = true;
    }
}
