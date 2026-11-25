package industries.angelic.salamander.cem.loading;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import net.minecraft.util.ResourceLocation;

/**
 * Resource-pack priority wins; ties prefer EMF directories and then per-model subdirectories. A modded model,
 * {@code <namespace>:<name>}, lives in its namespace's folders, or in EMF's older {@code modded/<namespace>/} folder.
 */
public final class CemModelPaths {

    private CemModelPaths() {}

    public static ResourceLocation select(String name, Predicate<ResourceLocation> exists,
        ToIntFunction<ResourceLocation> priority) {
        ResourceLocation selected = null;
        int highest = Integer.MIN_VALUE;
        for (ResourceLocation candidate : candidates(name)) {
            if (!exists.test(candidate)) continue;
            int rank = priority.applyAsInt(candidate);
            if (rank >= highest) {
                selected = candidate;
                highest = rank;
            }
        }
        return selected;
    }

    /** Every place the model may be, least preferred first. */
    static List<ResourceLocation> candidates(String name) {
        int colon = name.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : name.substring(0, colon), file = name.substring(colon + 1);
        List<ResourceLocation> candidates = new ArrayList<>();
        if (!namespace.equals("minecraft")) add(candidates, "minecraft", "modded/" + namespace + "/" + file);
        add(candidates, namespace, file);
        return candidates;
    }

    private static void add(List<ResourceLocation> candidates, String namespace, String file) {
        String last = file.substring(file.lastIndexOf('/') + 1);
        for (String directory : new String[] { "optifine/cem/", "emf/cem/" })
            for (String path : new String[] { file, file + "/" + last })
                candidates.add(new ResourceLocation(namespace, directory + path + ".jem"));
    }
}
