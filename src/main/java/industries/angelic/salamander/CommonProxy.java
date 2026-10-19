package industries.angelic.salamander;

import com.geckolib.animation.RawAnimation;
import com.geckolib.network.GeckoLibNetwork;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import industries.angelic.salamander.cem.network.CemNetwork;
import industries.angelic.salamander.cem.network.CemSignalPacket;
import industries.angelic.salamander.config.CemConfig;
import industries.angelic.salamander.config.DebugConfig;
import industries.angelic.salamander.debug.DebugContent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        try {
            ConfigurationManager.registerConfig(DebugConfig.class);
            ConfigurationManager.registerConfig(CemConfig.class);
            ConfigurationManager.registerConfig(industries.angelic.salamander.config.TextureConfig.class);
        } catch (ConfigException exception) {
            throw new IllegalStateException("Unable to register Salamander configuration", exception);
        }

        Salamander.LOG.info("Loading {} {}", Salamander.MODNAME, Tags.VERSION);
        GeckoLibNetwork.init();
        CemNetwork.init();

        if (DebugConfig.debugMode) DebugContent.register();
    }

    public void handleEntityAnimationTrigger(int entityId, boolean replacedEntity, String controllerName,
        String animationName) {}

    public void handleStopTriggeredEntityAnimation(int entityId, boolean replacedEntity, String controllerName,
        String animationName) {}

    public void handleSingletonAnimationTrigger(String syncableId, long instanceId, String controllerName,
        String animationName) {}

    public void handleStopTriggeredSingletonAnimation(String syncableId, long instanceId, String controllerName,
        String animationName) {}

    public void handleBlockEntityAnimationTrigger(int x, int y, int z, String controllerName, String animationName) {}

    public void handleStopTriggeredBlockEntityAnimation(int x, int y, int z, String controllerName,
        String animationName) {}

    public void handleStatelessEntityAnimationPlay(int entityId, boolean replacedEntity, RawAnimation animation) {}

    public void handleStatelessEntityAnimationStop(int entityId, boolean replacedEntity, String animation) {}

    public void handleStatelessSingletonAnimationPlay(String syncableId, long instanceId, RawAnimation animation) {}

    public void handleStatelessSingletonAnimationStop(String syncableId, long instanceId, String animation) {}

    public void handleStatelessBlockEntityAnimationPlay(int x, int y, int z, RawAnimation animation) {}

    public void handleStatelessBlockEntityAnimationStop(int x, int y, int z, String animation) {}

    public void handleCitadelAnimation(int entityId, int animationIndex) {}

    public void handleCemRegistration(Object manager, boolean registered) {}

    public void handleCemHello(Object handler, boolean enabled) {}

    public void handleCemSignal(Object handler, CemSignalPacket packet) {}
}
