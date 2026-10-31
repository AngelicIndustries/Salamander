package industries.angelic.salamander.skin.skull;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.ResourceLocation;

import com.mojang.authlib.GameProfile;

import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.api.skin.PlayerSkins;
import industries.angelic.salamander.skin.voxel.ImageVoxelTexture;
import industries.angelic.salamander.skin.voxel.VoxelBuilder;
import industries.angelic.salamander.skin.voxel.VoxelCube;
import industries.angelic.salamander.skin.voxel.VoxelCubeCollector;
import industries.angelic.salamander.skin.voxel.VoxelMesh;
import industries.angelic.salamander.skin.voxel.VoxelSurfaceBuilder;

/**
 * Static utility that generates 3D meshes from a player's skin BufferedImage.
 * <p>
 * A head's mesh is built again when its skin texture or that texture's pixels change, so refreshed skins show without
 * being told.
 */
public class SkinLayers3DSetup {

    /** Cache for skull meshes keyed by GameProfile. */
    private static final Map<UUID, SkullMeshCache> skullCache = new ConcurrentHashMap<>();

    public static void clearSkullCache() {
        skullCache.values()
            .forEach(cached -> { if (cached.mesh != null) cached.mesh.cleanup(); });
        skullCache.clear();
    }

    public static void updateSkullCache(UUID uuid, SkullMeshCache newCache) {
        if (uuid == null) return;
        SkullMeshCache oldCache = (newCache == null) ? skullCache.remove(uuid) : skullCache.put(uuid, newCache);
        if (oldCache != null && oldCache.mesh != null && oldCache != newCache) {
            oldCache.mesh.cleanup();
        }
    }

    public static SkullMeshCache getSkullCache(UUID uuid) {
        if (uuid == null) return null;
        return skullCache.get(uuid);
    }

    /**
     * Get or create a 3D hat mesh for a player skull.
     *
     * @param profile      the skull's game profile
     * @param skinLocation the skull's skin texture location
     * @return the hat mesh, or null if generation failed
     */
    public static VoxelMesh getOrCreateSkullMesh(GameProfile profile, ResourceLocation skinLocation) {
        if (profile == null || skinLocation == null) return null;

        BufferedImage skinImage = getSkinImage(skinLocation);
        SkullMeshCache cached = getSkullCache(profile.getId());
        if (cached != null) {
            if (skinLocation.equals(cached.skinLocation) && skinImage == cached.image) {
                return cached.mesh;
            }
            // Remove the entry along with the cleanup so a failed rebuild
            // cannot leave a destroyed mesh served under the old location.
            updateSkullCache(profile.getId(), null);
        }

        if (skinImage == null || skinImage.getWidth() != 64 || skinImage.getHeight() != 64) {
            return null;
        }

        try {
            ImageVoxelTexture skinData = new ImageVoxelTexture(skinImage);
            VoxelMesh mesh = buildMesh(skinData, 8, 8, 8, 32, 0, false, 0.6f);
            updateSkullCache(profile.getId(), new SkullMeshCache(skinLocation, skinImage, mesh));
            return mesh;
        } catch (Exception e) {
            Salamander.LOG.error("Failed to build 3D skull hat mesh", e);
            return null;
        }
    }

    private static VoxelMesh buildMesh(ImageVoxelTexture skinData, int width, int height, int depth, int textureU,
        int textureV, boolean topPivot, float rotationOffset) {
        VoxelCubeCollector builder = new VoxelCubeCollector();
        VoxelBuilder result = VoxelSurfaceBuilder
            .wrapBox(builder, skinData, width, height, depth, textureU, textureV, topPivot, rotationOffset);
        if (result == null || builder.isEmpty()) {
            return null;
        }
        List<VoxelCube> cubes = builder.getCubes();
        VoxelMesh mesh = new VoxelMesh(cubes);
        mesh.compileDisplayList();
        return mesh;
    }

    /** The skin's pixels, from whichever source loaded it. */
    private static BufferedImage getSkinImage(ResourceLocation skinLocation) {
        return PlayerSkins.image(skinLocation);
    }

    public static class SkullMeshCache {

        final ResourceLocation skinLocation;
        /** The pixels the mesh was built from, by identity. */
        final BufferedImage image;
        final VoxelMesh mesh;

        SkullMeshCache(ResourceLocation skinLocation, BufferedImage image, VoxelMesh mesh) {
            this.skinLocation = skinLocation;
            this.image = image;
            this.mesh = mesh;
        }
    }
}
