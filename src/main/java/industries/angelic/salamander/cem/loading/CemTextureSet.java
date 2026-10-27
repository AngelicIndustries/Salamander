package industries.angelic.salamander.cem.loading;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.minecraft.util.ResourceLocation;

/** Resource-only random texture discovery, independent of entity renderers and OpenGL. */
public final class CemTextureSet {

    public interface Resources {

        boolean exists(ResourceLocation location);

        int priority(ResourceLocation location);

        Properties properties(ResourceLocation location) throws IOException;
    }

    private static final CemRules.Selection BASE_SELECTION = new CemRules.Selection(1, 0);

    public final ResourceLocation base;
    public final ResourceLocation directory;
    public final CemRules rules;
    private final Resources resources;
    private final int count;
    private final List<ResourceLocation> bases;
    private final int minimumPriority;
    private final Map<Integer, ResourceLocation> textures = new HashMap<>();

    private CemTextureSet(ResourceLocation base, ResourceLocation directory, CemRules rules, int count,
        Resources resources, List<ResourceLocation> bases) {
        this.base = base;
        this.directory = directory;
        this.rules = rules;
        this.count = count;
        this.resources = resources;
        this.bases = bases;
        minimumPriority = bases.stream()
            .mapToInt(resources::priority)
            .max()
            .orElse(-1);
    }

    public static CemTextureSet load(List<ResourceLocation> bases, Resources resources) throws IOException {
        ResourceLocation original = bases.get(0), selected = null;
        int minimum = bases.stream()
            .mapToInt(resources::priority)
            .max()
            .orElse(-1);
        int highest = minimum;
        ResourceLocation base = original;
        boolean inheritedSelection = false;
        // Preferred directories are visited first; a higher pack always wins.
        for (ResourceLocation candidate : bases) {
            for (ResourceLocation directory : directories(candidate)) {
                ResourceLocation properties = properties(directory);
                ResourceLocation second = variant(directory, 2);
                int rank = Math.max(
                    resources.exists(properties) ? resources.priority(properties) : Integer.MIN_VALUE,
                    resources.exists(second) ? resources.priority(second) : Integer.MIN_VALUE);
                int directRank = rank;
                for (ResourceLocation parent : parents(directory)) {
                    ResourceLocation inherited = properties(parent);
                    if (resources.exists(inherited)) rank = Math.max(rank, resources.priority(inherited));
                }
                boolean inherited = rank > directRank;
                if (rank >= highest && (selected == null || rank > highest || inheritedSelection && !inherited)) {
                    selected = directory;
                    base = candidate;
                    highest = rank;
                    inheritedSelection = inherited;
                }
            }
        }
        if (selected == null) return new CemTextureSet(original, original, null, 1, resources, bases);
        CemRules rules = null;
        ResourceLocation properties = properties(selected);
        if (resources.exists(properties) && resources.priority(properties) >= minimum)
            rules = new CemRules(resources.properties(properties), true);
        if (rules == null) {
            ResourceLocation inherited = null;
            int rank = minimum;
            for (ResourceLocation candidate : bases) for (ResourceLocation directory : directories(candidate)) {
                for (ResourceLocation parent : parents(directory)) {
                    ResourceLocation parentProperties = properties(parent);
                    if (resources.exists(parentProperties) && resources.priority(parentProperties) >= rank
                        && (inherited == null || resources.priority(parentProperties) > rank)) {
                        inherited = parentProperties;
                        rank = resources.priority(parentProperties);
                    }
                }
            }
            if (inherited != null) rules = new CemRules(resources.properties(inherited), true);
        }
        int count = 1;
        if (rules == null) while (count < 4096 && resources.exists(variant(selected, count + 1))) count++;
        if (!resources.exists(base)) base = original;
        return new CemTextureSet(base, selected, rules, count, resources, bases);
    }

    public CemRules.Selection select(CemRules.Facts facts, CemRules.Selection inherited) {
        if (rules != null) return rules.select(facts);
        if (inherited != null) return inherited;
        return count == 1 ? BASE_SELECTION : new CemRules.Selection(1 + (int) Math.floorMod(facts.seed(), count), 0);
    }

    public ResourceLocation texture(CemRules.Selection choice) {
        if (choice.model <= 1) return base;
        return textures.computeIfAbsent(choice.model, this::findTexture);
    }

    private ResourceLocation findTexture(int index) {
        // An overlay may supply only the suffix selected by its body's rules, with no own rule file or #2.
        int highest = minimumPriority;
        ResourceLocation fallback = base;
        for (ResourceLocation candidate : bases) for (ResourceLocation location : directories(candidate)) {
            ResourceLocation variant = variant(location, index);
            if (resources.exists(variant) && resources.priority(variant) >= highest
                && (fallback.equals(base) || resources.priority(variant) > highest)) {
                fallback = variant;
                highest = resources.priority(variant);
            }
        }
        return fallback;
    }

    public static ResourceLocation variant(ResourceLocation base, int index) {
        String path = base.getResourcePath();
        String stem = path.substring(0, path.length() - 4);
        return new ResourceLocation(
            base.getResourceDomain(),
            stem + (Character.isDigit(stem.charAt(stem.length() - 1)) ? "." : "") + index + ".png");
    }

    private static ResourceLocation properties(ResourceLocation base) {
        String path = base.getResourcePath();
        return new ResourceLocation(base.getResourceDomain(), path.substring(0, path.length() - 4) + ".properties");
    }

    private static List<ResourceLocation> directories(ResourceLocation base) {
        String path = base.getResourcePath();
        if (!path.startsWith("textures/")) return Collections.singletonList(base);
        List<ResourceLocation> result = new ArrayList<>();
        String tail = path.substring("textures/".length());
        result.add(new ResourceLocation(base.getResourceDomain(), "etf/random/" + tail));
        result.add(new ResourceLocation(base.getResourceDomain(), "optifine/random/" + tail));
        if (tail.startsWith("entity/")) {
            result.add(new ResourceLocation(base.getResourceDomain(), "optifine/mob/" + tail.substring(7)));
            result.add(new ResourceLocation(base.getResourceDomain(), "mcpatcher/mob/" + tail.substring(7)));
        }
        result.add(base);
        return result;
    }

    private static List<ResourceLocation> parents(ResourceLocation base) {
        String path = base.getResourcePath();
        List<ResourceLocation> result = new ArrayList<>();
        for (String suffix : new String[] { "_angry_nectar", "_nectar", "_tame", "_angry", "_shooting", "_eyes",
            "_overlay", "_fur", "_invulnerable", "_exploding" }) {
            if (path.endsWith(suffix + ".png")) result.add(
                new ResourceLocation(
                    base.getResourceDomain(),
                    path.substring(0, path.length() - suffix.length() - 4) + ".png"));
        }
        return result;
    }
}
