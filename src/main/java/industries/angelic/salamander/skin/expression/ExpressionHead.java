package industries.angelic.salamander.skin.expression;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import industries.angelic.salamander.cem.client.CemSkinImage;
import industries.angelic.salamander.config.SkinLayers3DConfig;
import industries.angelic.salamander.skin.model.ModelPlayer;
import industries.angelic.salamander.skin.voxel.ImageVoxelTexture;
import industries.angelic.salamander.skin.voxel.VoxelMesh;
import industries.angelic.salamander.skin.voxel.VoxelMeshes;

/**
 * The editor's 3D head: the draft skin as a texture of its own, its face redrawn whenever the animation moves it, and
 * the hat layer in 3D as Salamander draws it on players (flat when 3D layers are off).
 */
final class ExpressionHead {

    private static final float SCALE = 0.0625F;

    private final ModelBase model = new ModelBase() {};
    private final ModelRenderer head;
    private final ModelRenderer flatHat;
    private DynamicTexture texture;
    private ResourceLocation location;
    /** The skin the texture and hat were made from, and the face last uploaded. */
    private BufferedImage source;
    private int[] skinPixels;
    private final int[] face = new int[64];
    private VoxelMesh hat;

    ExpressionHead() {
        this.head = new ModelRenderer(this.model, 0, 0).setTextureSize(64, 64);
        this.head.addBox(-4, -8, -4, 8, 8, 8);
        this.flatHat = new ModelRenderer(this.model, 32, 0).setTextureSize(64, 64);
        this.flatHat.addBox(-4, -8, -4, 8, 8, 8, 0.5F);
    }

    /**
     * Draws the head centered on x, y, size pixels across, turned by yaw and pitch (degrees; positive pitch shows the
     * top), with this face (8x8,
     * [x][y]) on it.
     */
    void draw(int x, int y, int size, float yaw, float pitch, BufferedImage skin, int[][] colors, int guiScale) {
        if (!update(skin, colors)) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_SCISSOR_BIT
                | GL11.GL_POLYGON_BIT);
        // In a world the GUI draws with back faces culled, and this mirrored GUI view would cull the front ones: draw
        // both, sorted by a depth that starts afresh inside the square.
        int left = x - size / 2, top = y - size / 2;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            left * guiScale,
            minecraft.displayHeight - (top + size) * guiScale,
            size * guiScale,
            size * guiScale);
        GL11.glDepthMask(true);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glPushMatrix();
        // The head is 8 model pixels across: half a block at scale 1/16, drawn size pixels wide.
        float blocks = size / 0.5F * 0.62F;
        GL11.glTranslatef(x, y, 150.0F);
        GL11.glScalef(blocks, blocks, blocks);
        GL11.glRotatef(-pitch, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(180.0F + yaw, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(0.0F, 4 * SCALE, 0.0F);
        minecraft.getTextureManager()
            .bindTexture(this.location);
        this.head.render(SCALE);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        if (this.hat != null) {
            // As Salamander's 3D layers scale the hat around the head's base.
            float voxel = SkinLayers3DConfig.headVoxelSize;
            GL11.glPushMatrix();
            GL11.glScalef(voxel, voxel, voxel);
            GL11.glTranslatef(0.0F, 0.6F * SCALE, 0.0F);
            this.hat.draw();
            GL11.glPopMatrix();
        } else {
            this.flatHat.render(SCALE);
        }
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Makes the texture and hat for a new skin, and uploads a new face; false without a usable skin. */
    private boolean update(BufferedImage skin, int[][] colors) {
        if (!ExpressionDraft.usable(skin)) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (this.texture == null) {
            this.texture = new DynamicTexture(64, 64);
            this.location = minecraft.getTextureManager()
                .getDynamicTextureLocation("salamander_expression_head", this.texture);
        }
        boolean upload = false;
        if (skin != this.source) {
            this.source = skin;
            BufferedImage normalized = CemSkinImage.normalize(skin);
            this.skinPixels = normalized.getRGB(0, 0, 64, 64, null, 0, 64);
            if (this.hat != null) this.hat.cleanup();
            this.hat = SkinLayers3DConfig.enabled3D && SkinLayers3DConfig.layers.enableHat3D
                ? VoxelMeshes.compile(new ImageVoxelTexture(normalized), ModelPlayer.HAT)
                : null;
            Arrays.fill(this.face, 1);
            upload = true;
        }
        int[] pixels = this.texture.getTextureData();
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) {
            if (this.face[y * 8 + x] == colors[x][y]) continue;
            this.face[y * 8 + x] = colors[x][y];
            upload = true;
        }
        if (upload) {
            System.arraycopy(this.skinPixels, 0, pixels, 0, pixels.length);
            for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) pixels[(8 + y) * 64 + 8 + x] = colors[x][y];
            this.texture.updateDynamicTexture();
        }
        return true;
    }

    /** Frees the texture and the hat's display list; they come back on the next draw. */
    void release() {
        if (this.location != null) Minecraft.getMinecraft()
            .getTextureManager()
            .deleteTexture(this.location);
        if (this.hat != null) this.hat.cleanup();
        this.texture = null;
        this.location = null;
        this.hat = null;
        this.source = null;
    }
}
