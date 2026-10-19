package industries.angelic.salamander.cem.loading;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import net.minecraft.util.ResourceLocation;

/** Resource-pack priority wins; ties prefer EMF directories and then per-model subdirectories. */
public final class CemModelPaths {

    private CemModelPaths() {}

    public static ResourceLocation select(String name, Predicate<ResourceLocation> exists,
        ToIntFunction<ResourceLocation> priority) {
        ResourceLocation selected = null;
        int highest = Integer.MIN_VALUE;
        for (String directory : new String[] { "optifine/cem/", "emf/cem/" }) {
            for (String path : new String[] { name, name + "/" + name }) {
                ResourceLocation candidate = new ResourceLocation("minecraft", directory + path + ".jem");
                if (!exists.test(candidate)) continue;
                int rank = priority.applyAsInt(candidate);
                if (rank >= highest) {
                    selected = candidate;
                    highest = rank;
                }
            }
        }
        return selected;
    }
}
