package industries.angelic.salamander.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.salamander.Salamander;

/** Player skulls' hat layer, drawn in 3D while {@link SkinLayers3DConfig#enabled3D} is on. */
@Config(modid = Salamander.MODID, category = "skulls", configSubDirectory = Salamander.MODID)
@Config.LangKeyPattern(pattern = "%mod.config.%cat.%field")
public final class SkullConfig {

    @Config.Comment("Enable 3D voxel rendering for player skulls.")
    @Config.DefaultBoolean(true)
    public static boolean enableSkulls = true;

    @Config.Comment("Skull voxel size scale.")
    @Config.DefaultFloat(1.1f)
    @Config.RangeFloat(min = 0.5f, max = 2.0f)
    public static float skullVoxelSize = 1.1f;

    private SkullConfig() {}
}
