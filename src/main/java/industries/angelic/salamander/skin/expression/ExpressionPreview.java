package industries.angelic.salamander.skin.expression;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import industries.angelic.salamander.cem.client.CemBinding;
import industries.angelic.salamander.cem.client.CemModelParts;
import industries.angelic.salamander.cem.client.CemPlayers;
import industries.angelic.salamander.cem.client.CemResources;
import industries.angelic.salamander.cem.client.CemRuntime;
import industries.angelic.salamander.cem.client.CemTargets;
import industries.angelic.salamander.cem.model.CemModel;
import industries.angelic.salamander.config.SkinLayers3DConfig;
import industries.angelic.salamander.skin.model.ModelPlayer;
import industries.angelic.salamander.skin.voxel.ImageVoxelTexture;
import industries.angelic.salamander.skin.voxel.VoxelCube;
import industries.angelic.salamander.skin.voxel.VoxelMeshes;

/**
 * The draft as it will look in game: a stand-in player wearing the draft skin, drawn by Salamander's player renderer
 * and so by the enabled Just Expressions or Fresh Moves pack model, framed on its head. It needs a world to stand in:
 * the client's, or the one Wawel Auth's preview player is in at the main menu.
 */
final class ExpressionPreview {

    /** A profile no provider knows, so nothing looks its skin up; the draft texture is its skin. */
    private static final UUID ID = UUID
        .nameUUIDFromBytes("salamander:expression-preview".getBytes(StandardCharsets.UTF_8));
    private static final long PACK_CHECK_MS = 1000L;
    /** RenderPlayer draws players at 15/16; model pixels are 1/16 block, the neck 24 of them above the feet. */
    private static final float PLAYER_SCALE = 0.9375F, PIXEL = 1.0F / 16.0F, NECK = 24.0078125F;
    /** How fast the size follows the turn. */
    private static final float SCALE_EASE_SECONDS = 0.25F;
    /** How much of the square the turned head fills. */
    private static final float FILL = 0.9F;

    private final RenderPlayer renderer = new RenderPlayer();
    private StandIn player;
    private boolean slimPlayer;
    private DynamicTexture texture;
    private ResourceLocation location;
    private BufferedImage shown;
    /** The shown skin's head bounds; see {@link #headBounds}. */
    private float[] bounds = { 4, -8, 0 };
    /** The size as drawn, in GUI pixels per block, and when; it eases toward the fit. */
    private float scale;
    private long scaledAt;
    private long checkedAt = Long.MIN_VALUE / 2;
    private boolean pack;

    /** The stand-in player: it wears the draft texture, has no cape, and is lit as a preview. */
    private static final class StandIn extends EntityOtherPlayerMP {

        ResourceLocation skin;

        StandIn(World world, GameProfile profile) {
            super(world, profile);
        }

        @Override
        public ResourceLocation getLocationSkin() {
            return this.skin;
        }

        @Override
        public ResourceLocation getLocationCape() {
            return null;
        }

        @Override
        public boolean func_152122_n() { // hasCape
            return false;
        }

        @Override
        public int getBrightnessForRender(float partialTicks) {
            return 0x00F000F0;
        }

        @Override
        public float getBrightness(float partialTicks) {
            return 1.0F;
        }
    }

    /** The world a preview can be drawn in, or null: the client's, else that of a preview player given. */
    static World world(AbstractClientPlayer preview) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world != null) return world;
        return preview == null ? null : preview.worldObj;
    }

    /**
     * Gets ready to draw this skin in this world: a new stand-in when the world or arm model changed, a new texture
     * when the skin did. False without a world or skin.
     */
    boolean prepare(World world, BufferedImage skin, boolean slim, String name) {
        if (world == null || skin == null || skin.getWidth() != 64 || skin.getHeight() != 64) return false;
        if (this.player == null || this.player.worldObj != world || this.slimPlayer != slim) {
            this.player = new StandIn(world, profile(name, slim));
            this.slimPlayer = slim;
            this.checkedAt = Long.MIN_VALUE / 2;
        }
        if (skin != this.shown) {
            // A new texture each time: skin caches follow the texture, and a new one is a new skin to them.
            release();
            this.texture = new DynamicTexture(64, 64);
            skin.getRGB(0, 0, 64, 64, this.texture.getTextureData(), 0, 64);
            this.texture.updateDynamicTexture();
            this.location = Minecraft.getMinecraft()
                .getTextureManager()
                .getDynamicTextureLocation("salamander_expression_preview", this.texture);
            this.shown = skin;
            this.bounds = headBounds(skin);
            this.checkedAt = Long.MIN_VALUE / 2;
        }
        this.player.skin = this.location;
        return true;
    }

    /** The profile's textures property tells the arm model, as a provider's would; it has no signature to trust. */
    private static GameProfile profile(String name, boolean slim) {
        GameProfile profile = new GameProfile(ID, name == null || name.isEmpty() ? "Player" : name);
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/0\""
            + (slim ? ",\"metadata\":{\"model\":\"slim\"}" : "")
            + "}}}";
        profile.getProperties()
            .put(
                "textures",
                new Property(
                    "textures",
                    Base64.getEncoder()
                        .encodeToString(json.getBytes(StandardCharsets.UTF_8))));
        return profile;
    }

    /**
     * Whether a pack model with eyes or brows draws the stand-in, as Just Expressions' and Fresh Moves' do; looked up
     * at most once a second.
     */
    boolean packShown() {
        if (this.player == null) return false;
        long now = Minecraft.getSystemTime();
        if (now - this.checkedAt < PACK_CHECK_MS) return this.pack;
        this.checkedAt = now;
        this.pack = false;
        try {
            if (!(this.renderer.modelBipedMain instanceof CemModelParts)) return false;
            CemPlayers.register(this.renderer.modelBipedMain);
            CemBinding binding = new CemBinding(
                this.renderer.modelBipedMain,
                ((CemModelParts) this.renderer.modelBipedMain).salamander$cemParts());
            ResourceLocation previous = CemRuntime.texture();
            CemResources.Selection selection;
            try {
                CemRuntime.texture(this.location);
                selection = CemResources.INSTANCE.select(
                    CemTargets.candidates(this.player, this.renderer.modelBipedMain, this.location),
                    binding,
                    this.player);
            } finally {
                CemRuntime.texture(previous);
            }
            if (selection == null) return false;
            for (CemModel.Node node : selection.entry.model.nodes) {
                String id = node.id == null ? "" : node.id.toLowerCase(Locale.ROOT);
                if (id.contains("eye") || id.contains("pupil") || id.contains("brow")) return this.pack = true;
            }
        } catch (RuntimeException | LinkageError unavailable) {
            // Without CEM's answer, the editor's own head shows.
        }
        return false;
    }

    /**
     * The head's bounds around the neck, in model pixels (y down), for this skin: the head, and its hat as drawn,
     * from the hat's own voxels in 3D (scaled up from the head's base as Salamander's layers draw it) or the flat hat
     * where it has pixels. Just Expressions' and Fresh Moves' head parts lie on the face, inside these.
     *
     * @return half width (and half depth), top and bottom
     */
    static float[] headBounds(BufferedImage skin) {
        float half = 4, top = -8, bottom = 0;
        if (SkinLayers3DConfig.enabled3D && SkinLayers3DConfig.layers.enableHat3D) {
            float voxel = SkinLayers3DConfig.headVoxelSize;
            for (VoxelCube cube : VoxelMeshes.cubes(new ImageVoxelTexture(skin), ModelPlayer.HAT)) {
                half = Math.max(
                    half,
                    voxel * Math.max(
                        Math.max(Math.abs(cube.minX), Math.abs(cube.maxX)),
                        Math.max(Math.abs(cube.minZ), Math.abs(cube.maxZ))));
                top = Math.min(top, voxel * (cube.minY + 0.6F));
                bottom = Math.max(bottom, voxel * (cube.maxY + 0.6F));
            }
        } else {
            boolean hat = false;
            for (int x = 32; x < 64 && !hat; x++)
                for (int y = 0; y < 16 && !hat; y++) hat = (skin.getRGB(x, y) >>> 24) != 0;
            if (hat) return new float[] { 4.5F, -8.5F, 0.5F };
        }
        return new float[] { half, top, bottom };
    }

    /**
     * Draws the stand-in's face centered in the square x, y, size (GUI pixels), seen level from face height and as
     * large as the turned head fits. yaw turns the whole player (positive turns its face right as seen), pitch tilts
     * the view (positive shows the top); lookYaw and lookPitch turn the head alone, around its neck, toward the
     * pointer.
     */
    void draw(int x, int y, int size, float yaw, float pitch, float lookYaw, float lookPitch, int guiScale) {
        if (this.player == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        StandIn entity = this.player;
        entity.ticksExisted = (int) (Minecraft.getSystemTime() / 50L);
        float cx = x + size / 2.0F, cy = y + size / 2.0F;
        // Players turn their face right as seen here by a smaller yaw.
        entity.renderYawOffset = entity.prevRenderYawOffset = -yaw;
        entity.rotationYaw = entity.prevRotationYaw = -yaw;
        entity.rotationYawHead = entity.prevRotationYawHead = -yaw - lookYaw;
        entity.rotationPitch = entity.prevRotationPitch = lookPitch;
        entity.limbSwing = entity.limbSwingAmount = entity.prevLimbSwingAmount = 0;

        RenderManager manager = RenderManager.instance;
        World world = manager.worldObj;
        EntityLivingBase living = manager.livingPlayer;
        Entity viewing = manager.field_147941_i;
        float viewY = manager.playerViewY, viewX = manager.playerViewX;
        double viewerX = manager.viewerPosX, viewerY = manager.viewerPosY, viewerZ = manager.viewerPosZ;
        EntityLivingBase view = minecraft.renderViewEntity;
        int displayHeight = minecraft.displayHeight;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            manager.cacheActiveRenderInfo(
                entity.worldObj,
                minecraft.getTextureManager(),
                minecraft.fontRenderer,
                entity,
                entity,
                minecraft.gameSettings,
                0.0F);
            minecraft.renderViewEntity = entity;
            this.renderer.setRenderManager(manager);
            // Only the head shows: the body is cut off at the square, whose depth starts afresh.
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(x * guiScale, displayHeight - (y + size) * guiScale, size * guiScale, size * guiScale);
            GL11.glDepthMask(true);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glPushMatrix();
            // The head's middle at the square's middle, seen level; as big as the head turned this way fits.
            float[] bounds = this.bounds;
            float half = bounds[0], height = bounds[2] - bounds[1];
            double turn = Math.toRadians(yaw + lookYaw), tilt = Math.toRadians(pitch + lookPitch);
            float wide = (float) (2 * half * (Math.abs(Math.cos(turn)) + Math.abs(Math.sin(turn))));
            float tall = (float) (height * Math.abs(Math.cos(tilt)) + 2 * half * Math.abs(Math.sin(tilt)));
            // The size follows the turn gently, so turning doesn't pump it.
            float target = size * FILL / (Math.max(wide, tall) * PIXEL * PLAYER_SCALE);
            long now = System.nanoTime();
            float seconds = this.scaledAt == 0 ? 1 : Math.min(1, (now - this.scaledAt) / 1.0e9f);
            this.scaledAt = now;
            this.scale = this.scale <= 0 ? target
                : this.scale + (target - this.scale) * (1 - (float) Math.exp(-seconds / SCALE_EASE_SECONDS));
            float scale = this.scale;
            float middle = (NECK - (bounds[1] + bounds[2]) / 2) * PIXEL * PLAYER_SCALE;
            GL11.glTranslatef(cx, cy, 100.0F);
            GL11.glScalef(-scale, scale, scale);
            GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            RenderHelper.enableStandardItemLighting();
            GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            GL11.glTranslatef(0.0F, -middle + entity.yOffset, 0.0F);
            manager.playerViewY = 180.0F;
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.renderer.doRender((AbstractClientPlayer) entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
            GL11.glPopMatrix();
        } finally {
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glPopAttrib();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            manager.worldObj = world;
            manager.livingPlayer = living;
            manager.field_147941_i = viewing;
            manager.playerViewY = viewY;
            manager.playerViewX = viewX;
            manager.viewerPosX = viewerX;
            manager.viewerPosY = viewerY;
            manager.viewerPosZ = viewerZ;
            minecraft.renderViewEntity = view;
        }
    }

    /** Frees the draft texture; the next prepare makes another. */
    void release() {
        if (this.location != null) Minecraft.getMinecraft()
            .getTextureManager()
            .deleteTexture(this.location);
        this.texture = null;
        this.location = null;
        this.shown = null;
    }
}
