package industries.angelic.salamander.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.salamander.Salamander;

@Config(modid = Salamander.MODID, category = "cem", configSubDirectory = Salamander.MODID)
@Config.LangKeyPattern(pattern = "%mod.config.%cat.%field")
public final class CemConfig {

    @Config.Comment("Install Salamander's custom entity model hooks. When false, no CEM code runs: resource-pack entity models, player models, capes and EFR elytra, and CEM random and emissive textures are ignored.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean hooks = true;

    @Config.Comment("Load custom entity models for supported vanilla and Et Futurum Requiem targets from resource packs. Reload resources after changing.")
    @Config.DefaultBoolean(true)
    public static boolean enabled = true;

    @Config.Comment("Load player models, capes, and EFR elytra from resource packs. Reload resources after changing.")
    @Config.DefaultBoolean(true)
    public static boolean playerModels = true;

    @Config.Comment("Render emissive textures associated with custom entity models. Reload resources after changing.")
    @Config.DefaultBoolean(true)
    public static boolean emissiveTextures = true;

    @Config.Comment("Optionally synchronize mob aggression and melee attacks to clients supporting Salamander CEM. Does not change gameplay.")
    @Config.DefaultBoolean(true)
    public static boolean serverAnimationSignals = true;

    private CemConfig() {}
}
