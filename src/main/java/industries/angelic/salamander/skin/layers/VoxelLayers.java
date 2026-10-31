package industries.angelic.salamander.skin.layers;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.api.skin.LayerSegment;
import industries.angelic.salamander.api.skin.PlayerSkin;
import industries.angelic.salamander.api.skin.SkinPart;
import industries.angelic.salamander.config.SkinLayers3DConfig;
import industries.angelic.salamander.skin.voxel.ImageVoxelTexture;
import industries.angelic.salamander.skin.voxel.VoxelMesh;
import industries.angelic.salamander.skin.voxel.VoxelMeshes;
import industries.angelic.salamander.skin.voxel.VoxelSurfaceBuilder;
import industries.angelic.salamander.skin.voxel.VoxelTexture;

/**
 * Voxel (3D) layers. Meshes are built per player skin revision and segment, freed when the revision changes or the
 * player has not been drawn for a while. Meshes without the texels worn things cover are built per coverage mask, and
 * freed once that mask has not been drawn for a while.
 */
@SideOnly(Side.CLIENT)
final class VoxelLayers {

    enum Result {
        DRAWN,
        /** The segment has no visible texels; nothing to draw, flat or voxel. */
        EMPTY,
        /** No usable 64x64 image yet; draw flat. */
        UNAVAILABLE
    }

    private static final long EVICT_AFTER_MS = 30_000L;
    private static final long SWEEP_EVERY_MS = 5_000L;

    private static final Map<UUID, ProfileMeshes> PROFILES = new HashMap<>();
    private static long lastSweep;

    private VoxelLayers() {}

    /** {@code mask} is what worn things cover of the segment: its texels are left out. */
    static Result draw(PlayerSkin skin, LayerSegment segment, float scale, Coverage.Mask mask) {
        ProfileMeshes meshes = PROFILES.get(skin.profileId());
        if (meshes == null || meshes.revision != skin.revision()) {
            if (meshes != null) meshes.cleanup();
            meshes = new ProfileMeshes(skin);
            PROFILES.put(skin.profileId(), meshes);
        }
        long now = System.currentTimeMillis();
        meshes.lastUsed = now;

        if (!meshes.usable()) return Result.UNAVAILABLE;
        VoxelMesh mesh = mask.none() ? meshes.mesh(segment) : meshes.masked(segment, mask, now);
        if (mesh == null) return Result.EMPTY;
        render(mesh, segment, scale);
        return Result.DRAWN;
    }

    static void sweepIfDue() {
        long now = System.currentTimeMillis();
        if (now - lastSweep < SWEEP_EVERY_MS) return;
        lastSweep = now;
        Iterator<ProfileMeshes> iterator = PROFILES.values()
            .iterator();
        while (iterator.hasNext()) {
            ProfileMeshes meshes = iterator.next();
            if (now - meshes.lastUsed > EVICT_AFTER_MS) {
                meshes.cleanup();
                iterator.remove();
            } else {
                meshes.sweepMasked(now);
            }
        }
        OverlayBoxes.sweep(now);
    }

    static void clear() {
        for (ProfileMeshes meshes : PROFILES.values()) meshes.cleanup();
        PROFILES.clear();
        OverlayBoxes.clearMasked();
    }

    /**
     * Scales the voxels around their box, with the per-part values 3D Skin Layers uses: horizontally around the box
     * center, vertically around the box end attached to the body (the top, or the bottom for the hat). Slices use the
     * whole box, so the halves of a jointed limb scale like one limb.
     */
    private static void render(VoxelMesh mesh, LayerSegment segment, float scale) {
        Tuning tuning = Tuning.of(segment.part());
        float centerX = segment.originX() + segment.width() / 2.0F;
        float centerZ = segment.originZ() + segment.depth() / 2.0F;
        float pivotY = segment.part() == SkinPart.HAT ? segment.originY() + segment.height() : segment.originY();

        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX * scale, (pivotY + tuning.offsetY) * scale, centerZ * scale);
        GL11.glScalef(tuning.scaleX, tuning.scaleY, tuning.scaleZ);
        GL11.glTranslatef(-centerX * scale, (tuning.preOffsetY - pivotY) * scale, -centerZ * scale);
        if (scale != OverlayBoxes.MODEL_SCALE) {
            float factor = scale / OverlayBoxes.MODEL_SCALE;
            GL11.glScalef(factor, factor, factor);
        }
        mesh.draw();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private static final class Tuning {

        final float scaleX;
        final float scaleY;
        final float scaleZ;
        final float offsetY;
        final float preOffsetY;

        private Tuning(float scaleX, float scaleY, float scaleZ, float offsetY, float preOffsetY) {
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.offsetY = offsetY;
            this.preOffsetY = preOffsetY;
        }

        static Tuning of(SkinPart part) {
            float base = SkinLayers3DConfig.baseVoxelSize;
            return switch (part) {
                case HAT -> {
                    float head = SkinLayers3DConfig.headVoxelSize;
                    yield new Tuning(head, head, head, 0.0F, 0.6F);
                }
                case JACKET -> new Tuning(SkinLayers3DConfig.bodyVoxelWidthSize, 1.035F, base, -0.2F, 0.0F);
                case LEFT_SLEEVE, RIGHT_SLEEVE -> new Tuning(base, 1.035F, base, -0.1F, 0.0F);
                case LEFT_PANTS, RIGHT_PANTS -> new Tuning(base, 1.035F, base, -0.2F, 0.0F);
                case CAPE -> new Tuning(base, base, base, 0.0F, 0.0F);
            };
        }
    }

    private static final class ProfileMeshes {

        final long revision;
        private final PlayerSkin skin;
        private final Map<LayerSegment, VoxelMesh> meshes = new HashMap<>();
        private final Map<Coverage.Mask, Masked> masked = new HashMap<>();
        private ImageVoxelTexture texture;
        private boolean unusable;
        long lastUsed;

        ProfileMeshes(PlayerSkin skin) {
            this.skin = skin;
            this.revision = skin.revision();
        }

        /** Voxel layers need the exact 64x64 layout. */
        boolean usable() {
            if (texture == null && !unusable) {
                BufferedImage image = skin.normalizedImage();
                if (image != null && image.getWidth() == 64 && image.getHeight() == 64) {
                    texture = new ImageVoxelTexture(image);
                } else {
                    unusable = true;
                }
            }
            return texture != null;
        }

        /** Null when the segment has no visible texels. */
        VoxelMesh mesh(LayerSegment segment) {
            if (!meshes.containsKey(segment)) {
                meshes.put(segment, VoxelMeshes.compile(texture, segment));
            }
            return meshes.get(segment);
        }

        /** Null when none of the segment's texels are visible or left uncovered. */
        VoxelMesh masked(LayerSegment segment, Coverage.Mask mask, long now) {
            Masked under = masked.get(mask);
            if (under == null) {
                under = new Masked(new MaskedTexture(texture, mask));
                masked.put(mask, under);
            }
            under.lastUsed = now;
            if (!under.meshes.containsKey(segment)) {
                under.meshes.put(segment, VoxelMeshes.compile(under.texture, segment));
            }
            return under.meshes.get(segment);
        }

        void sweepMasked(long now) {
            Iterator<Masked> iterator = masked.values()
                .iterator();
            while (iterator.hasNext()) {
                Masked under = iterator.next();
                if (now - under.lastUsed > EVICT_AFTER_MS) {
                    under.cleanup();
                    iterator.remove();
                }
            }
        }

        void cleanup() {
            for (VoxelMesh mesh : meshes.values()) {
                if (mesh != null) mesh.cleanup();
            }
            meshes.clear();
            for (Masked under : masked.values()) under.cleanup();
            masked.clear();
        }
    }

    /** The meshes built under one coverage mask. */
    private static final class Masked {

        final VoxelTexture texture;
        final Map<LayerSegment, VoxelMesh> meshes = new HashMap<>();
        long lastUsed;

        Masked(VoxelTexture texture) {
            this.texture = texture;
        }

        void cleanup() {
            for (VoxelMesh mesh : meshes.values()) {
                if (mesh != null) mesh.cleanup();
            }
            meshes.clear();
        }
    }

    /** A skin without the texels a mask covers. */
    private static final class MaskedTexture implements VoxelTexture {

        private final VoxelTexture skin;
        private final Coverage.Mask mask;

        MaskedTexture(VoxelTexture skin, Coverage.Mask mask) {
            this.skin = skin;
            this.mask = mask;
        }

        @Override
        public boolean isPresent(VoxelSurfaceBuilder.UV uv) {
            return skin.isPresent(uv) && !mask.covers(uv.u, uv.v);
        }

        @Override
        public boolean isSolid(VoxelSurfaceBuilder.UV uv) {
            return skin.isSolid(uv) && !mask.covers(uv.u, uv.v);
        }

        @Override
        public int getWidth() {
            return skin.getWidth();
        }

        @Override
        public int getHeight() {
            return skin.getHeight();
        }
    }
}
