package industries.angelic.salamander.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.salamander.Salamander;

@Config(modid = Salamander.MODID, category = "debug", configSubDirectory = Salamander.MODID)
@Config.LangKeyPattern(pattern = "%mod.config.%cat.%field")
public final class DebugConfig {

    @Config.Comment("Register Salamander's development-only model test entities. Must match on client and server.")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean debugMode = false;

    private DebugConfig() {}
}
