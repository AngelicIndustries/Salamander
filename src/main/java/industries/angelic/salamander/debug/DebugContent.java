package industries.angelic.salamander.debug;

import cpw.mods.fml.common.registry.EntityRegistry;
import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.debug.entity.DebugCitadelDragon;
import industries.angelic.salamander.debug.entity.DebugCitadelFly;
import industries.angelic.salamander.debug.entity.DebugModelEntity;

/** Opt-in entities used to validate Salamander's supported model formats in game. */
public final class DebugContent {

    private DebugContent() {}

    public static void register() {
        EntityRegistry.registerModEntity(DebugModelEntity.class, "debug_model", 0, Salamander.instance, 64, 1, true);
        EntityRegistry
            .registerModEntity(DebugCitadelFly.class, "debug_citadel_fly", 1, Salamander.instance, 64, 1, true);
        EntityRegistry
            .registerModEntity(DebugCitadelDragon.class, "debug_citadel_dragon", 2, Salamander.instance, 128, 1, true);
        Salamander.LOG.info("Registered Salamander debug model entities");
    }
}
