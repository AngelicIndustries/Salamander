package industries.angelic.salamander;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = Salamander.MODID,
    version = Tags.VERSION,
    name = Salamander.MODNAME,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:gtnhlib@[0.11.30,)",
    customProperties = { @Mod.CustomProperty(k = "license", v = "MIT AND LGPL-3.0-only"),
        @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/AngelicIndustries/Salamander/issues") })
public class Salamander {

    public static final String MODID = "salamander";
    public static final String MODNAME = "Salamander";
    public static final String MODGROUP = "industries.angelic";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static Salamander instance;

    @SidedProxy(
        clientSide = MODGROUP + "." + MODID + ".ClientProxy",
        serverSide = MODGROUP + "." + MODID + ".CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }
}
