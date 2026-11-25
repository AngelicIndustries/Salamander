package industries.angelic.salamander.cem.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;

/**
 * The models a biped renderer draws its wearer's armor with, which EMF replaces with {@code <mob>_inner_armor} and
 * {@code <mob>_outer_armor}. Only a renderer's own armor models count: an item's custom armor model, or an animation
 * mod's stand-in for one, keeps its own geometry.
 */
public final class CemArmor {

    public static final String INNER = "inner";
    public static final String OUTER = "outer";

    /** The leggings slot of {@code shouldRenderPass}, the one drawn with the thinner inner model. */
    private static final int LEGGINGS = 2;

    private static final Map<ModelBase, String> LAYERS = new WeakHashMap<>();
    private static final Set<String> MODELS = Collections.synchronizedSet(
        new HashSet<>(
            java.util.Arrays.asList(
                "net.minecraft.client.model.ModelBiped",
                "net.minecraft.client.model.ModelZombie",
                "net.minecraft.client.model.ModelZombieVillager",
                "ganymedes01.etfuturum.client.model.ModelArmorStandArmor")));

    private CemArmor() {}

    /** Lets armor packs replace armor models of this class, when a renderer draws armor with one. */
    public static void register(String modelClass) {
        MODELS.add(modelClass);
    }

    /**
     * A renderer offered Forge its own armor model for a slot and got {@code used} back: the same model unless the item
     * brings its own.
     */
    public static void offer(ModelBiped own, int slot, ModelBiped used) {
        if (own != null && own == used
            && MODELS.contains(
                own.getClass()
                    .getName()))
            LAYERS.put(own, slot == LEGGINGS ? INNER : OUTER);
    }

    /** {@link #INNER} or {@link #OUTER} for a renderer's own armor model, else null. */
    public static String layer(ModelBase model) {
        return model == null ? null : LAYERS.get(model);
    }
}
