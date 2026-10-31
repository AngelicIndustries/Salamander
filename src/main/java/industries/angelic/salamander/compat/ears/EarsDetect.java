package industries.angelic.salamander.compat.ears;

import java.io.IOException;

import net.minecraft.launchwrapper.Launch;

import industries.angelic.salamander.Salamander;

/**
 * Whether Ears draws players itself: standalone Ears, its Plateau build, or the copy FoamFix bundles when FoamFix
 * applies its Ears patch. Ears then owns the player model and its skin layers.
 */
public final class EarsDetect {

    private static final String STANDALONE_EARS_CLASS = "com.unascribed.ears.Ears";
    private static final String PLATEAU_EARS_CLASS = "diy.y2k.five.plateau.Plateau";
    private static final String FOAMFIX_TRANSFORMER_CLASS = "pl.asie.foamfix.bugfixmod.coremod.BugfixModClassTransformer";

    private static volatile Boolean active;

    private EarsDetect() {}

    public static boolean isRendererActive() {
        Boolean known = active;
        if (known == null) {
            known = classExists(PLATEAU_EARS_CLASS) || classExists(STANDALONE_EARS_CLASS) || foamFixEarsActive();
            active = known;
        }
        return known;
    }

    private static boolean foamFixEarsActive() {
        if (!classExists(FOAMFIX_TRANSFORMER_CLASS)) return false;
        try {
            Class<?> transformerClass = Class.forName(FOAMFIX_TRANSFORMER_CLASS, false, Launch.classLoader);
            Object transformer = transformerClass.getField("instance")
                .get(null);
            return transformer != null && (Boolean) transformerClass.getMethod("applyEarsPatch")
                .invoke(transformer);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Salamander.LOG.warn("Could not determine whether FoamFix's bundled Ears renderer is active", e);
            return false;
        }
    }

    private static boolean classExists(String name) {
        try {
            return Launch.classLoader.getClassBytes(name) != null;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }
}
