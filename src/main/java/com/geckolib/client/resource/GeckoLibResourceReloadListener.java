package com.geckolib.client.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import com.geckolib.GeckoLibConstants;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibLoader;
import com.geckolib.loading.math.MathParser;
import com.gtnewhorizon.gtnhlib.client.model.loading.BackingResourceManager;
import com.gtnewhorizon.gtnhlib.client.model.loading.GlobalResourceManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.mixins.early.minecraft.client.resources.AccessorAbstractResourcePack;

/** Client resource reload bridge using GTNHLib's resource-manager accessors. */
@SideOnly(Side.CLIENT)
public final class GeckoLibResourceReloadListener implements IResourceManagerReloadListener {

    public static final GeckoLibResourceReloadListener INSTANCE = new GeckoLibResourceReloadListener();

    private GeckoLibResourceReloadListener() {}

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        ResourceFiles resources = findResources(resourceManager);
        Map<ResourceLocation, BakedGeoModel> models = new LinkedHashMap<>();
        Map<ResourceLocation, BakedAnimations> animations = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> modelSources = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> animationSources = new LinkedHashMap<>();

        for (ResourceLocation location : sorted(resources.models)) {
            try (Reader reader = openReader(resourceManager, location)) {
                GeckoLibLoader loader = GeckoLibResources.findLoader(location);

                putLoadedResource(models, modelSources, location, loader.loadModel(location, reader), "geometry");
            } catch (Exception exception) {
                GeckoLibConstants.LOGGER.error("Error loading geometry resource '{}'", location, exception);
            }
        }

        MathParser mathParser = MathParser.createWithDeduplication();

        for (ResourceLocation location : sorted(resources.animations)) {
            try (Reader reader = openReader(resourceManager, location)) {
                GeckoLibLoader loader = GeckoLibResources.findLoader(location);

                putLoadedResource(
                    animations,
                    animationSources,
                    location,
                    loader.loadAnimations(location, reader, mathParser),
                    "animation");
            } catch (Exception exception) {
                GeckoLibConstants.LOGGER.error("Error loading animation resource '{}'", location, exception);
            }
        }

        int legacyModels = countLegacySources(modelSources);
        int legacyAnimations = countLegacySources(animationSources);

        if (legacyModels != 0 || legacyAnimations != 0) GeckoLibConstants.LOGGER.info(
            "Loaded {} legacy-path geometry models and {} legacy-path animation files",
            legacyModels,
            legacyAnimations);

        GeckoLibResources.apply(models, animations);
    }

    private static ResourceFiles findResources(IResourceManager resourceManager) {
        ResourceFiles resources = new ResourceFiles();

        if (!(resourceManager instanceof GlobalResourceManager)) {
            GeckoLibConstants.LOGGER
                .error("GTNHLib resource-manager accessor is unavailable; GeckoLib resources cannot be enumerated");
            return resources;
        }

        Set<IResourcePack> packs = new HashSet<>();

        for (IResourceManager domainManager : ((GlobalResourceManager) resourceManager)
            .nhlib$getDomainResourceManagers()
            .values()) {
            if (domainManager instanceof BackingResourceManager)
                packs.addAll(((BackingResourceManager) domainManager).nhlib$getResourcePacks());
        }

        for (IResourcePack pack : packs) {
            scanPack(pack, resources);
        }

        return resources;
    }

    private static void scanPack(IResourcePack pack, ResourceFiles resources) {
        if (!(pack instanceof AbstractResourcePack) || !(pack instanceof AccessorAbstractResourcePack)) return;

        File root = ((AccessorAbstractResourcePack) pack).salamander$getResourcePackFile();

        try {
            if (root.isDirectory()) scanFolder(root.toPath(), resources);
            else if (root.isFile()) scanArchive(root, resources);
        } catch (Exception exception) {
            GeckoLibConstants.LOGGER.warn("Unable to scan resource pack '{}'", root, exception);
        }
    }

    private static void scanFolder(Path root, ResourceFiles resources) throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .forEach(
                    path -> classify(
                        normalize(
                            root.relativize(path)
                                .toString()),
                        resources));
        }
    }

    private static void scanArchive(File archive, ResourceFiles resources) throws IOException {
        try (ZipFile zip = new ZipFile(archive)) {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (!entry.isDirectory()) classify(entry.getName(), resources);
            }
        }
    }

    private static void classify(String path, ResourceFiles resources) {
        if (!path.startsWith("assets/") || !GeckoLibResources.isFileTypeSupported(path)) return;

        String[] components = path.split("/", 3);

        if (components.length != 3 || components[1].isEmpty()) return;

        String resourcePath = components[2];
        ResourceLocation location = new ResourceLocation(components[1], resourcePath);

        if (GeckoLibResources.isModelResourcePath(resourcePath)) resources.models.add(location);
        else if (GeckoLibResources.isAnimationResourcePath(resourcePath)) resources.animations.add(location);
    }

    private static Reader openReader(IResourceManager resourceManager, ResourceLocation location) throws IOException {
        IResource resource = resourceManager.getResource(location);
        InputStream stream = resource.getInputStream();

        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    private static List<ResourceLocation> sorted(Set<ResourceLocation> locations) {
        List<ResourceLocation> sorted = new ArrayList<>(locations);

        sorted.sort(
            Comparator
                .comparingInt((ResourceLocation location) -> GeckoLibResources.isLegacyResourcePath(location) ? 0 : 1)
                .thenComparing(ResourceLocation::toString));

        return sorted;
    }

    private static <T> void putLoadedResource(Map<ResourceLocation, T> resources,
        Map<ResourceLocation, ResourceLocation> sources, ResourceLocation source, T resource, String type) {
        ResourceLocation key = GeckoLibResources.stripPrefixAndSuffix(source);
        ResourceLocation previousSource = sources.put(key, source);

        resources.put(key, resource);

        if (previousSource != null) GeckoLibConstants.LOGGER
            .warn("{} resource '{}' replaces '{}' for logical resource '{}'", type, source, previousSource, key);
    }

    private static int countLegacySources(Map<ResourceLocation, ResourceLocation> sources) {
        int count = 0;

        for (ResourceLocation source : sources.values()) {
            if (GeckoLibResources.isLegacyResourcePath(source)) count++;
        }

        return count;
    }

    private static String normalize(String path) {
        return path.replace(File.separatorChar, '/');
    }

    private static final class ResourceFiles {

        private final Set<ResourceLocation> models = new HashSet<>();
        private final Set<ResourceLocation> animations = new HashSet<>();
    }
}
