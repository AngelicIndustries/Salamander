package industries.angelic.salamander.cem.client;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.Loader;
import industries.angelic.salamander.config.CemConfig;

/** Client rendering proxies for beds, which were baked blocks before 1.12. Never added to a world. */
public final class CemBeds {

    private static final Map<Long, Bed> BEDS = new ConcurrentHashMap<>();
    private static final Map<Block, ResourceLocation> TEXTURES = new IdentityHashMap<>();
    private static final BedModel MODEL = new BedModel();
    private static final List<String> TARGET = Collections.singletonList("bed");
    private static volatile boolean available;
    private static boolean enabled;
    private static World world;

    private CemBeds() {}

    public static void register() {
        ClientRegistry.bindTileEntitySpecialRenderer(Bed.class, new Renderer());
        CemTargets.register(Bed.class.getName(), "bed");
    }

    public static void reload() {
        available = false;
        BEDS.clear();
        for (ResourceLocation texture : TEXTURES.values()) if (texture.getResourcePath()
            .startsWith("dynamic/"))
            Minecraft.getMinecraft()
                .getTextureManager()
                .deleteTexture(texture);
        TEXTURES.clear();
        available = CemResources.INSTANCE.select(TARGET, new CemBinding(MODEL, MODEL.salamander$cemParts()), MODEL)
            != null;
    }

    public static void frame() {
        Minecraft mc = Minecraft.getMinecraft();
        if (world != mc.theWorld) {
            world = mc.theWorld;
            BEDS.clear();
        }
        boolean next = available && CemConfig.enabled;
        if (enabled != next) {
            enabled = next;
            if (mc.theWorld != null && mc.renderGlobal != null) mc.renderGlobal.loadRenderers();
        }
    }

    /** Called by either vanilla or Angelica's chunk workers; no GL or mutable resource caches here. */
    public static boolean track(Block block, int x, int y, int z) {
        if (!available || !CemConfig.enabled || !(block instanceof BlockBed)) return false;
        long key = ((long) x & 0x3ffffffL) << 38 | ((long) z & 0x3ffffffL) << 12 | (y & 0xfffL);
        BEDS.computeIfAbsent(key, ignored -> new Bed(x, y, z));
        return true;
    }

    public static void render(EntityLivingBase camera, ICamera frustum, float partial) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!enabled || world == null || BEDS.isEmpty() || MinecraftForgeClient.getRenderPass() != 0) return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT);
        mc.entityRenderer.enableLightmap(partial);
        RenderHelper.enableStandardItemLighting();
        boolean angelica = Loader.isModLoaded("angelica");
        if (angelica) CemBedShaders.begin();
        try {
            Iterator<Map.Entry<Long, Bed>> iterator = BEDS.entrySet()
                .iterator();
            while (iterator.hasNext()) {
                Bed bed = iterator.next()
                    .getValue();
                if (!world.blockExists(bed.xCoord, bed.yCoord, bed.zCoord)
                    || !(world.getBlock(bed.xCoord, bed.yCoord, bed.zCoord) instanceof BlockBed)) {
                    iterator.remove();
                    continue;
                }
                if (camera.getDistanceSq(bed.xCoord + .5, bed.yCoord + .5, bed.zCoord + .5) > 4096
                    || !frustum.isBoundingBoxInFrustum(bed.getRenderBoundingBox())) continue;
                bed.setWorldObj(world);
                bed.updateContainingBlockInfo();
                TileEntityRendererDispatcher.instance.renderTileEntity(bed, partial);
            }
        } finally {
            if (angelica) CemBedShaders.end();
            mc.entityRenderer.disableLightmap(partial);
            GL11.glPopAttrib();
        }
    }

    public static final class Bed extends TileEntity {

        Bed(int x, int y, int z) {
            xCoord = x;
            yCoord = y;
            zCoord = z;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            tag.setInteger("x", xCoord);
            tag.setInteger("y", yCoord);
            tag.setInteger("z", zCoord);
        }

        @Override
        public AxisAlignedBB getRenderBoundingBox() {
            return AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord - 1, zCoord - 1, xCoord + 2, yCoord + 2, zCoord + 2);
        }
    }

    private static final class BedModel extends ModelBase implements CemModelParts {

        final Map<String, ModelRenderer> parts = new LinkedHashMap<>();

        BedModel() {
            textureWidth = 64;
            textureHeight = 64;
            parts.put("head", new ModelRenderer(this, 0, 0).addBox(0, 0, 0, 16, 16, 6));
            parts.put("foot", new ModelRenderer(this, 0, 22).addBox(0, 0, 0, 16, 16, 6));
            int[][] boxes = { { 0, -16 }, { 0, 0 }, { -16, -16 }, { -16, 0 } };
            float[] rotations = { 0, (float) Math.PI / 2, (float) Math.PI * 1.5f, (float) Math.PI };
            for (int i = 0; i < 4; i++) {
                ModelRenderer leg = new ModelRenderer(this, 50, i * 6).addBox(boxes[i][0], 6, boxes[i][1], 3, 3, 3);
                leg.rotateAngleX = (float) Math.PI / 2;
                leg.rotateAngleZ = rotations[i];
                parts.put("leg" + new int[] { 3, 1, 4, 2 }[i], leg);
            }
        }

        @Override
        public Map<String, ModelRenderer> salamander$cemParts() {
            return parts;
        }

        void render(boolean head) {
            parts.get("head").showModel = head;
            parts.get("foot").showModel = !head;
            for (int i = 0; i < 4; i++) parts.get("leg" + (i + 1)).showModel = (i < 2) == head;
            CemRuntime.Draw previous = CemRuntime.begin(this, null, 0, 0, 0, 0, 0);
            try {
                for (ModelRenderer part : parts.values()) part.render(.0625f);
            } finally {
                CemRuntime.end(previous, .0625f);
            }
        }
    }

    private static final class Renderer extends TileEntitySpecialRenderer {

        @Override
        public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partial) {
            int meta = tile.getBlockMetadata(), facing = meta & 3;
            bindTexture(TEXTURES.computeIfAbsent(tile.getBlockType(), CemBeds::texture));
            GL11.glPushMatrix();
            try {
                GL11.glTranslated(
                    x + (facing == 0 || facing == 3 ? 1 : 0),
                    y + .5625,
                    z + (facing == 0 || facing == 1 ? 1 : 0));
                GL11.glRotatef(90, 1, 0, 0);
                GL11.glRotatef(new float[] { 180, -90, 0, 90 }[facing], 0, 0, 1);
                MODEL.render((meta & 8) != 0);
            } finally {
                GL11.glPopMatrix();
            }
        }
    }

    /** Reconstruct the modern atlas from the selected pack's legacy block textures. */
    private static ResourceLocation texture(Block block) {
        String name = String.valueOf(Block.blockRegistry.getNameForObject(block));
        String color = name.substring(name.indexOf(':') + 1)
            .replace("_bed", "");
        if (color.equals("bed")) color = "red";
        ResourceLocation modern = new ResourceLocation("minecraft", "textures/entity/bed/" + color + ".png");
        if (CemResources.INSTANCE.exists(modern)) return modern;
        BufferedImage first = icon(block, 1, 8);
        int factor = Math.max(1, first.getWidth() / 16);
        BufferedImage atlas = new BufferedImage(64 * factor, 64 * factor, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        graphics.scale(factor, factor);
        BufferedImage wood = icon(net.minecraft.init.Blocks.planks, 1, 0);
        for (int half = 0; half < 2; half++) {
            int meta = half == 0 ? 8 : 0;
            BufferedImage top = icon(block, 1, meta), side = icon(block, 4, meta),
                end = icon(block, half == 0 ? 3 : 2, meta);
            int y = half * 22;
            // Bed block tops run across the image; entity-atlas tops run along it.
            Graphics2D rotated = (Graphics2D) graphics.create();
            rotated.translate(6, y + 22);
            rotated.rotate(-Math.PI / 2);
            rotated.drawImage(top, 0, 0, 16, 16, null);
            rotated.dispose();
            graphics.drawImage(wood, 28, y + 6, 16, 16, null);
            for (int x : new int[] { 0, 22 }) {
                Graphics2D edge = (Graphics2D) graphics.create();
                edge.translate(x, y + 22);
                edge.rotate(-Math.PI / 2);
                edge.drawImage(
                    side,
                    0,
                    0,
                    16,
                    6,
                    0,
                    side.getHeight() * 13 / 16,
                    side.getWidth(),
                    side.getHeight() * 7 / 16,
                    null);
                edge.dispose();
            }
            graphics.drawImage(
                end,
                6,
                y,
                22,
                y + 6,
                0,
                end.getHeight() * 13 / 16,
                end.getWidth(),
                end.getHeight() * 7 / 16,
                null);
            graphics.drawImage(
                end,
                22,
                y,
                38,
                y + 6,
                0,
                end.getHeight() * 13 / 16,
                end.getWidth(),
                end.getHeight() * 7 / 16,
                null);
        }
        for (int i = 0; i < 4; i++) graphics.drawImage(wood, 50, i * 6, 12, 6, null);
        graphics.dispose();
        return Minecraft.getMinecraft()
            .getTextureManager()
            .getDynamicTextureLocation("salamander_bed", new DynamicTexture(atlas));
    }

    private static BufferedImage icon(Block block, int side, int meta) {
        try {
            ResourceLocation icon = new ResourceLocation(
                block.getIcon(side, meta)
                    .getIconName());
            ResourceLocation resource = new ResourceLocation(
                icon.getResourceDomain(),
                "textures/blocks/" + icon.getResourcePath() + ".png");
            try (InputStream stream = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(resource)
                .getInputStream()) {
                return ImageIO.read(stream);
            }
        } catch (Exception ignored) {
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
    }
}
