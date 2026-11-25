package industries.angelic.salamander.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.salamander.Salamander;

@Config(modid = Salamander.MODID, category = "cem", configSubDirectory = Salamander.MODID)
@Config.LangKeyPattern(pattern = "%mod.config.%cat.%field")
public final class CemConfig {

    @Config.Comment("Install Salamander's custom entity model hooks. When false, no CEM code runs: resource-pack entity models, player models, capes and EFR elytra, random entity textures and emissive textures are ignored. Player skins and GeckoLib models are not affected.")
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

    @Config.Comment("Compile resource-pack animations to JVM bytecode instead of interpreting them. Same results, less CPU per animated model. Reload resources after changing.")
    @Config.DefaultBoolean(true)
    public static boolean compiledAnimations = true;

    @Config.Comment("Draw each part tree of a pack model in as few draws as possible, transforming the parts on the CPU. Same image, less CPU. Player models with skin layers keep per-part draws.")
    @Config.DefaultBoolean(true)
    public static boolean batchedGeometry = true;

    @Config.Comment("With Angelica's entity batching on, hand pack model parts to Angelica's batcher, as vanilla parts are, instead of drawing them at once. Angelica then keeps batching the renderers that draw pack models. Turned on in game, it reaches renderers Angelica already stopped batching after a restart.")
    @Config.DefaultBoolean(true)
    public static boolean angelicaBatching = true;

    @Config.Comment("Write a .jem model of each other mod's entity model when it is first drawn, with its part names, pivots, boxes and texture coordinates, to salamander/cem-export/<modid>/. A starting point for resource packs; see the log for each file written.")
    @Config.DefaultBoolean(false)
    public static boolean exportModels = false;

    @Config.Comment("Optionally synchronize mob aggression and melee attacks to clients supporting Salamander CEM. Does not change gameplay.")
    @Config.DefaultBoolean(true)
    public static boolean serverAnimationSignals = true;

    private CemConfig() {}
}
