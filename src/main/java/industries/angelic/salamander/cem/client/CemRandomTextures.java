package industries.angelic.salamander.cem.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
            CemTextureSet set = SETS.get(original);
            if (set == null && !SETS.containsKey(original)) {
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
            long tick = CemRuleFacts.tick(object);
            State state = STATES.get(object);
            if (state == null) {
                // Capture initial biome/height/blocks at the same first texture bind as before.
                state = new State(CemRuleFacts.get(object), tick);
                STATES.put(object, state);
            } else if (state.tick != tick) {
                state.tick = tick;
                state.snapshot = null;
                state.selections.clear();
            }
            Selected selected = state.selections.get(original);
            if (selected == null) {
                // Numbered/default textures use only the stable seed or inherited choice.
                // Preserve per-tick selection invalidation, but do not gather unused world facts.
                if (set.rules != null && state.snapshot == null) state.snapshot = CemRuleFacts.get(object);
                CemRules.Selection choice = set.select(state, CemRuntime.textureChoice());
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

    private static final class State implements CemRules.Facts {

        final String biome, height, block, blockInside;
        final List<String> blocks;
        final long seed;
        final Map<ResourceLocation, Selected> selections = new HashMap<>();
        long tick;
        CemRuleFacts snapshot;

        State(CemRuleFacts facts, long tick) {
            this.tick = tick;
            snapshot = facts;
            seed = facts.seed();
            biome = facts.value("biomes");
            height = facts.value("heights");
            block = facts.value("blocks");
            blockInside = facts.value("blocksInside");
            blocks = block == null
                ? blockInside == null ? Collections.emptyList() : Collections.singletonList(blockInside)
                : blockInside == null ? Collections.singletonList(block)
                    : Collections.unmodifiableList(Arrays.asList(block, blockInside));
        }

        @Override
        public String value(String key) {
            switch (key) {
                case "biomes":
                    return biome;
                case "heights":
                    return height;
                case "blocks":
                    return block;
                case "blocksInside":
                    return blockInside;
                default:
                    return snapshot.value(key);
            }
        }

        @Override
        public List<String> values(String key) {
            return key.equals("blocks") ? blocks : snapshot.values(key);
        }

        @Override
        public List<String> nbt(String path, boolean raw) {
            return snapshot.nbt(path, raw);
        }

        @Override
        public long seed() {
            return seed;
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
