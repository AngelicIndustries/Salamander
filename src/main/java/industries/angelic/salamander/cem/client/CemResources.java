package industries.angelic.salamander.cem.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.loading.CemModelPaths;
import industries.angelic.salamander.cem.model.CemModel;
import industries.angelic.salamander.config.CemConfig;
import industries.angelic.salamander.mixins.early.minecraft.client.resources.AccessorAbstractResourcePack;

/** Atomic client reload snapshot. Registered adapters enumerate their resources without scanning unrelated packs. */
public final class CemResources implements IResourceManagerReloadListener {

    private static java.util.List<net.minecraft.client.resources.IResourcePack> packs = Collections.emptyList();

    public static void packs(java.util.List<net.minecraft.client.resources.IResourcePack> value) {
        packs = new java.util.ArrayList<>(value);
    }

    static int priority(ResourceLocation texture) {
        for (int i = packs.size() - 1; i >= 0; i--) if (packs.get(i)
            .resourceExists(texture)) return i;
        return -1;
    }

    public static final CemResources INSTANCE = new CemResources();
    private IResourceManager manager;
    private String suffix;
    private final Map<String, Entry> definitions = new HashMap<>();
    private final Map<String, ResourceLocation> modelPaths = new HashMap<>();
    private final Map<ResourceLocation, Boolean> available = new HashMap<>();
    private final java.util.List<ResourceLocation> generatedTextures = new java.util.ArrayList<>();
    private final Map<ResourceLocation, ResourceLocation> textures = new HashMap<>();
    private final Map<ResourceLocation, Variants> variants = new HashMap<>();
    private final Set<CemModel> failed = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean models = true;

    private CemResources() {}

    public boolean hasModels() {
        return models;
    }

    private static boolean scanModels() {
        for (IResourcePack pack : packs) if (hasModels(pack)) return true;
        return false;
    }

    private static boolean hasModels(IResourcePack pack) {
        if (pack instanceof DefaultResourcePack) return false;
        if (!(pack instanceof AbstractResourcePack)) return true;
        Set<String> domains = pack.getResourceDomains();
        if (domains != null && !domains.contains("minecraft")) return false;
        File root = ((AccessorAbstractResourcePack) pack).salamander$getResourcePackFile();
        if (root == null) return true;
        if (root.isDirectory()) return hasEntries(new File(root, "assets/minecraft/optifine/cem"))
            || hasEntries(new File(root, "assets/minecraft/emf/cem"));
        try (ZipFile zip = new ZipFile(root)) {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement()
                    .getName();
                if (name.startsWith("assets/minecraft/optifine/cem/") || name.startsWith("assets/minecraft/emf/cem/"))
                    return true;
            }
            return false;
        } catch (IOException exception) {
            return true;
        }
    }

    private static boolean hasEntries(File directory) {
        String[] names = directory.list();
        return names != null && names.length > 0;
    }

    public boolean hasTargets(java.util.List<String> names) {
        for (String name : names) if (modelPath(name) != null) return true;
        return false;
    }

    private ResourceLocation modelPath(String name) {
        if (!modelPaths.containsKey(name))
            modelPaths.put(name, CemModelPaths.select(name, this::exists, CemResources::priority));
        return modelPaths.get(name);
    }

    public Selection select(java.util.List<String> candidates, CemBinding binding, Object subject) {
        if (!CemConfig.enabled || manager == null) return null;
        for (String name : candidates) {
            ResourceLocation resource = modelPath(name);
            if (resource == null) continue;
            Variants choices = variants.computeIfAbsent(resource, this::variants);
            industries.angelic.salamander.cem.loading.CemRules.Selection choice = choices
                .select(CemRuleFacts.get(subject));
            ResourceLocation selected = choice.model == 1 ? resource : variant(resource, choice.model);
            Entry entry = entry(exists(selected) ? selected : resource, binding);
            if (entry != null) return new Selection(entry, choice.rule);
        }
        return null;
    }

    private Entry entry(ResourceLocation resource, CemBinding binding) {
        String key = resource + "|"
            + binding.nativeModel.getClass()
                .getName()
            + "|"
            + binding.pivots.keySet()
            + "|"
            + binding.parents;
        if (!definitions.containsKey(key)) {
            Entry entry = null;
            try {
                CemLoader loader = new CemLoader(
                    location -> new InputStreamReader(
                        manager.getResource(location)
                            .getInputStream(),
                        StandardCharsets.UTF_8));
                CemModel model = loader
                    .load(resource, binding.pivots, binding.parents, binding.textureWidth, binding.textureHeight);
                if (!loader.missingReferences()
                    .isEmpty())
                    Salamander.LOG.warn(
                        "CEM '{}' references missing {}; those parts keep only their own boxes",
                        resource,
                        loader.missingReferences());
                ResourceLocation texture = model.texture == null ? CemRuntime.texture() : model.texture;
                entry = new Entry(model, texture, emissive(texture));
                Salamander.LOG.info(
                    "Loaded CEM '{}' ({} parts, {} animation assignments)",
                    model.source,
                    model.nodes.size(),
                    model.assignmentCount());
            } catch (Exception exception) {
                Salamander.LOG.error("Unable to load CEM '{}'; retaining native model", resource, exception);
            }
            definitions.put(key, entry);
        }
        Entry entry = definitions.get(key);
        return entry != null && !failed.contains(entry.model) ? entry : null;
    }

    private Variants variants(ResourceLocation resource) {
        ResourceLocation properties = new ResourceLocation(
            resource.getResourceDomain(),
            resource.getResourcePath()
                .replaceAll("\\.jem$", ".properties"));
        if (exists(properties)) try (InputStream stream = manager.getResource(properties)
            .getInputStream()) {
                Properties values = new Properties();
                values.load(stream);
                return new Variants(new industries.angelic.salamander.cem.loading.CemRules(values), new int[] { 1 });
            } catch (Exception exception) {
                Salamander.LOG.warn("Invalid CEM rules '{}'; using base model", properties, exception);
                return new Variants(null, new int[] { 1 });
            }
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        indices.add(1);
        for (int index = 2, missing = 0; index <= 4096 && missing < 10; index++) {
            if (exists(variant(resource, index))) {
                indices.add(index);
                missing = 0;
            } else missing++;
        }
        return new Variants(
            null,
            indices.stream()
                .mapToInt(Integer::intValue)
                .toArray());
    }

    private static ResourceLocation variant(ResourceLocation base, int index) {
        String path = base.getResourcePath();
        return new ResourceLocation(base.getResourceDomain(), path.substring(0, path.length() - 4) + index + ".jem");
    }

    public ResourceLocation texture(ResourceLocation original) {
        if (original == null) return null;
        return textures.computeIfAbsent(original, source -> {
            {
                int nativePack = priority(source);
                for (String path : CemTextureAliases.candidates(source)) {
                    ResourceLocation candidate = new ResourceLocation("minecraft", path);
                    if (!exists(candidate)) continue;
                    int modernPack = priority(candidate);
                    if (modernPack >= nativePack) {
                        if (source.getResourcePath()
                            .startsWith("textures/entity/villager/") && !source.equals(candidate))
                            return villagerTexture(source, candidate);
                        return candidate;
                    }
                }
            }
            return source;
        });
    }

    private ResourceLocation villagerTexture(ResourceLocation legacy, ResourceLocation modern) {
        try (java.io.InputStream oldStream = manager.getResource(legacy)
            .getInputStream();
            java.io.InputStream newStream = manager.getResource(modern)
                .getInputStream()) {
            java.awt.image.BufferedImage old = javax.imageio.ImageIO.read(oldStream),
                base = javax.imageio.ImageIO.read(newStream);
            int size = Math.max(old.getWidth(), base.getWidth());
            java.awt.image.BufferedImage result = new java.awt.image.BufferedImage(
                size,
                size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = result.createGraphics();
            g.drawImage(base, 0, 0, size, size, null);
            // 1.7 stores profession robes in a complete atlas. Preserve those clothes when the modern pack only
            // replaces the base skin.
            g.drawImage(
                old,
                0,
                size * 20 / 64,
                size,
                size,
                0,
                old.getHeight() * 20 / 64,
                old.getWidth(),
                old.getHeight(),
                null);
            g.drawImage(
                base,
                size * 13 / 64,
                size * 19 / 64,
                size * 21 / 64,
                size * 24 / 64,
                base.getWidth() * 13 / 64,
                base.getHeight() * 19 / 64,
                base.getWidth() * 21 / 64,
                base.getHeight() * 24 / 64,
                null);
            g.dispose();
            ResourceLocation texture = net.minecraft.client.Minecraft.getMinecraft()
                .getTextureManager()
                .getDynamicTextureLocation(
                    "salamander_villager",
                    new net.minecraft.client.renderer.texture.DynamicTexture(result));
            generatedTextures.add(texture);
            return texture;
        } catch (IOException exception) {
            return modern;
        }
    }

    public boolean isEmissive(ResourceLocation texture) {
        return suffix != null && texture != null
            && texture.getResourcePath()
                .endsWith(suffix + ".png");
    }

    public ResourceLocation emissive(ResourceLocation texture) {
        texture = texture(CemRandomTextures.resolve(texture));
        if (suffix == null || texture == null
            || !texture.getResourcePath()
                .endsWith(".png"))
            return null;
        String path = texture.getResourcePath();
        ResourceLocation result = new ResourceLocation(
            texture.getResourceDomain(),
            path.substring(0, path.length() - 4) + suffix + ".png");
        return exists(result) ? result : null;
    }

    public boolean exists(ResourceLocation resource) {
        return manager != null && available.computeIfAbsent(resource, location -> exists(manager, location));
    }

    public void fail(CemModel model, Exception exception) {
        if (failed.add(model)) Salamander.LOG
            .error("Disabled CEM '{}' until resource reload after animation/render failure", model.source, exception);
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        for (ResourceLocation generated : generatedTextures) net.minecraft.client.Minecraft.getMinecraft()
            .getTextureManager()
            .deleteTexture(generated);
        generatedTextures.clear();
        this.manager = manager;
        models = scanModels();
        suffix = emissiveSuffix(manager);
        definitions.clear();
        modelPaths.clear();
        available.clear();
        variants.clear();
        textures.clear();
        failed.clear();
        CemRandomTextures.reload(manager);
        CemClient.clearAnimationState();
        CemRuntime.clear();
        CemChestHalves.clear();
        CemChests.clear();
        CemBeds.reload();
    }

    public static final class Selection {

        public final Entry entry;
        public final int rule;

        Selection(Entry entry, int rule) {
            this.entry = entry;
            this.rule = rule;
        }
    }

    private static final class Variants {

        final industries.angelic.salamander.cem.loading.CemRules rules;
        final int[] models;

        Variants(industries.angelic.salamander.cem.loading.CemRules rules, int[] models) {
            this.rules = rules;
            this.models = models;
        }

        industries.angelic.salamander.cem.loading.CemRules.Selection select(CemRuleFacts facts) {
            return rules != null ? rules.select(facts)
                : new industries.angelic.salamander.cem.loading.CemRules.Selection(
                    models[(int) Math.floorMod(facts.seed(), models.length)],
                    0);
        }
    }

    private static String emissiveSuffix(IResourceManager manager) {
        if (!CemConfig.emissiveTextures) return null;
        try (InputStream stream = manager.getResource(new ResourceLocation("minecraft", "optifine/emissive.properties"))
            .getInputStream()) {
            Properties properties = new Properties();
            properties.load(stream);
            String suffix = properties.getProperty("suffix.emissive", "")
                .trim();
            return suffix.matches("_[A-Za-z0-9_]+") ? suffix : null;
        } catch (FileNotFoundException ignored) {
            return null;
        } catch (IOException exception) {
            Salamander.LOG.warn("Unable to read CEM emissive properties", exception);
            return null;
        }
    }

    private static boolean exists(IResourceManager manager, ResourceLocation location) {
        try (InputStream ignored = manager.getResource(location)
            .getInputStream()) {
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public static final class Entry {

        public final CemModel model;
        public final ResourceLocation texture;
        public final ResourceLocation emissive;
        public final CemGeometry geometry;

        Entry(CemModel model, ResourceLocation texture, ResourceLocation emissive) {
            this.model = model;
            this.texture = texture;
            this.emissive = emissive;
            this.geometry = new CemGeometry(model);
        }
    }
}
