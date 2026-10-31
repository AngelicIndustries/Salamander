package industries.angelic.salamander.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.salamander.Salamander;

/** How players' skins draw, flat or 3D. */
@Config(modid = Salamander.MODID, category = "skins", configSubDirectory = Salamander.MODID)
@Config.LangKeyPattern(pattern = "%mod.config.%cat.%field")
public final class SkinConfig {

    @Config.Comment("Disable skin overlay rendering when armor is equipped.")
    @Config.DefaultBoolean(false)
    public static boolean hideOverlayArmor = false;

    @Config.Comment("Draw semi-transparent skin layer pixels see-through, in order with water, ice and stained glass, also under Angelica's shaders. Experimental.")
    @Config.DefaultBoolean(true)
    public static boolean translucentLayers = true;

    @Config.Comment("With translucentLayers, also draw semi-transparent base skin pixels see-through, on Salamander's and Even More Bends' player models. Experimental.")
    @Config.DefaultBoolean(true)
    public static boolean translucentBase = true;

    private SkinConfig() {}
}
