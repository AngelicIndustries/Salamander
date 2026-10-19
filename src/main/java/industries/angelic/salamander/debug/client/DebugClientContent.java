package industries.angelic.salamander.debug.client;

import cpw.mods.fml.client.registry.RenderingRegistry;
import industries.angelic.salamander.debug.entity.DebugCitadelDragon;
import industries.angelic.salamander.debug.entity.DebugCitadelFly;
import industries.angelic.salamander.debug.entity.DebugModelEntity;

public final class DebugClientContent {

    private DebugClientContent() {}

    public static void register() {
        RenderingRegistry.registerEntityRenderingHandler(DebugModelEntity.class, new DebugModelRenderer());
        RenderingRegistry.registerEntityRenderingHandler(DebugCitadelFly.class, new DebugCitadelFlyRenderer());
        RenderingRegistry.registerEntityRenderingHandler(DebugCitadelDragon.class, new DebugCitadelDragonRenderer());
    }
}
