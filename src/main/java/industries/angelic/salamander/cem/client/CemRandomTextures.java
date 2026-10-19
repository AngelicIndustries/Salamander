package industries.angelic.salamander.cem.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import com.google.common.collect.MapMaker;

import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.cem.loading.CemRules;
import industries.angelic.salamander.cem.loading.CemTextureSet;
import industries.angelic.salamander.config.TextureConfig;

/** Texture selection also runs for native models, without requiring a CEM definition. */
public final class CemRandomTextures {

    private static final Map<ResourceLocation, CemTextureSet> SETS = new HashMap<>();
    private static final Set<ResourceLocation> VARIANTS = new HashSet<>();
    private static final Map<Object, State> STATES = new MapMaker().weakKeys()
        .makeMap();
    private static CemTextureSet.Resources resources;

    private CemRandomTextures() {}

    public static void reload(IResourceManager manager) {
        SETS.clear();
        VARIANTS.clear();
        clearEntities();
        resources = new CemTextureSet.Resources() {

            @Override
            public boolean exists(ResourceLocation location) {
                return CemResources.INSTANCE.exists(location);
            }

            @Override
            public int priority(ResourceLocation location) {
                return CemResources.priority(location);
            }

            @Override
            public Properties properties(ResourceLocation location) throws IOException {
                try (InputStream stream = manager.getResource(location)
                    .getInputStream()) {
                    Properties result = new Properties();
                    result.load(stream);
                    return result;
                }
            }
        };
    }

    public static void clearEntities() {
        STATES.clear();
    }

    public static ResourceLocation resolve(ResourceLocation original) {
        Object object = CemRuntime.subject();
        if (!TextureConfig.randomEntityTextures || resources == null
            || original == null
            || object instanceof EntityPlayer
            || (!(object instanceof Entity) && !(object instanceof TileEntity))
            || CemItemContext.hand()
            || CemItemContext.gui()
            || !eligible(original)
            || VARIANTS.contains(original)
            || CemResources.INSTANCE.isEmissive(original)) return original;
        try {
            CemTextureSet set;
            if (SETS.containsKey(original)) set = SETS.get(original);
            else {
                List<ResourceLocation> candidates = new ArrayList<>();
                candidates.add(original);
                if (original.getResourceDomain()
                    .equals("minecraft"))
                    for (String alias : CemTextureAliases.candidates(original))
                        candidates.add(new ResourceLocation("minecraft", alias));
                set = CemTextureSet.load(candidates, resources);
                SETS.put(original, set);
            }
            if (set == null) return original;
            CemRuleFacts current = CemRuleFacts.get(object);
            State state = STATES.computeIfAbsent(object, ignored -> new State(current));
            if (state.snapshot != current) {
                state.snapshot = current;
                state.facts = state.facts(current);
                state.selections.clear();
            }
            Selected selected = state.selections.get(original);
            if (selected == null) {
                CemRules.Selection choice = set.select(state.facts, CemRuntime.textureChoice());
                selected = new Selected(choice, set.texture(choice));
                state.selections.put(original, selected);
                if (!selected.texture.equals(set.base) && choice.model > 1) VARIANTS.add(selected.texture);
            }
            CemRuntime.textureChoice(selected.choice);
            return selected.texture;
        } catch (Exception exception) {
            SETS.put(original, null);
            Salamander.LOG
                .warn("Invalid random entity texture rules for '{}'; retaining base texture", original, exception);
            return original;
        }
    }

    private static boolean eligible(ResourceLocation texture) {
        String path = texture.getResourcePath();
        if (!path.endsWith(".png")) return false;
        if (!texture.getResourceDomain()
            .equals("minecraft"))
            return path.startsWith("textures/") && !path.startsWith("textures/items/")
                && !path.startsWith("textures/blocks/")
                && !path.startsWith("textures/gui/");
        return path.startsWith("textures/entity/");
    }

    private static final class State {

        final Map<String, String> initial = new HashMap<>();
        final long seed;
        final Map<ResourceLocation, Selected> selections = new HashMap<>();
        CemRuleFacts snapshot;
        CemRules.Facts facts;

        State(CemRuleFacts facts) {
            seed = facts.seed();
            for (String key : new String[] { "biomes", "heights", "blocks", "blocksInside" })
                initial.put(key, facts.value(key));
        }

        CemRules.Facts facts(CemRuleFacts current) {
            return new CemRules.Facts() {

                @Override
                public String value(String key) {
                    return initial.containsKey(key) ? initial.get(key) : current.value(key);
                }

                @Override
                public List<String> values(String key) {
                    if (key.equals("blocks")) {
                        List<String> blocks = new ArrayList<>();
                        if (initial.get("blocks") != null) blocks.add(initial.get("blocks"));
                        if (initial.get("blocksInside") != null) blocks.add(initial.get("blocksInside"));
                        return blocks;
                    }
                    return current.values(key);
                }

                @Override
                public List<String> nbt(String path, boolean raw) {
                    return current.nbt(path, raw);
                }

                @Override
                public long seed() {
                    return seed;
                }
            };
        }
    }

    private static final class Selected {

        final CemRules.Selection choice;
        final ResourceLocation texture;

        Selected(CemRules.Selection choice, ResourceLocation texture) {
            this.choice = choice;
            this.texture = texture;
        }
    }
}
