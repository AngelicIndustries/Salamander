package industries.angelic.salamander.compat.ssb;

import java.lang.reflect.Field;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSkeletonHead;

import cpw.mods.fml.common.Loader;
import industries.angelic.salamander.Salamander;

/**
 * SimpleSkinBackport turns every biped into a 64x64 model in place and draws its own outer layers. With Salamander
 * loaded, Salamander draws players instead:
 * <ul>
 * <li>Salamander's player model answers SSB's model hooks ({@code ssb$set64x}, {@code ssb$setSlim}) itself, so SSB
 * adds no boxes or layer marks to it and swaps no arms.
 * <li>SSB's extruded layers are switched off, so other bipeds SSB converted match Salamander's players.
 * <li>SSB's hat on player heads is hidden while Salamander draws its own 3D hat there.
 * </ul>
 * Everything else SSB does, such as its default skins, stays.
 */
public final class SsbTakeover {

    private static final boolean LOADED = Loader.isModLoaded("simpleskinbackport");
    private static final String CONFIG = "roadhog360.simpleskinbackport.configuration.configs.ConfigMain";
    private static Field headwear;
    private static boolean headwearResolved;

    private SsbTakeover() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    /** After SSB read its config. */
    public static void takeOver() {
        if (!LOADED) return;
        try {
            Class.forName(CONFIG)
                .getField("extrudedLayers")
                .setBoolean(null, false);
            Salamander.LOG
                .info("SimpleSkinBackport is loaded: Salamander draws players, SSB's extruded layers are off");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Salamander.LOG.warn("Could not switch off SimpleSkinBackport's extruded layers", e);
        }
    }

    /** SSB's hat part on a head model, or null. */
    public static ModelRenderer headHat(ModelSkeletonHead model) {
        if (!LOADED || model == null) return null;
        if (!headwearResolved) {
            headwearResolved = true;
            try {
                headwear = ModelSkeletonHead.class.getField("ssb$bipedHeadwear");
            } catch (NoSuchFieldException | RuntimeException e) {
                Salamander.LOG.warn("SimpleSkinBackport's head hat was not found; player heads may show two hats", e);
            }
        }
        if (headwear == null) return null;
        try {
            return (ModelRenderer) headwear.get(model);
        } catch (ReflectiveOperationException | RuntimeException e) {
            headwear = null;
            return null;
        }
    }
}
