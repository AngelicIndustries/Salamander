package industries.angelic.salamander.skin.layers;

import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.api.skin.DrawKind;
import industries.angelic.salamander.api.skin.LayerDraw;
import industries.angelic.salamander.api.skin.LayerSegment;
import industries.angelic.salamander.api.skin.PlayerSkin;
import industries.angelic.salamander.api.skin.PlayerSkins;
import industries.angelic.salamander.api.skin.SkinPart;
import industries.angelic.salamander.api.skin.ViewerSkinSettings;
import industries.angelic.salamander.compat.ears.EarsAnchors;
import industries.angelic.salamander.compat.smartmoving.SmartMovingCompat;
import industries.angelic.salamander.skin.skull.SkinLayers3DSetup;
import industries.angelic.salamander.skin.translucent.LayerPainter;
import industries.angelic.salamander.skin.translucent.TranslucentLayers;

/** Implementation behind {@link industries.angelic.salamander.api.skin.SkinLayers}. Client thread only. */
@SideOnly(Side.CLIENT)
public final class LayerDraws {

    private static final DrawStack<Draw> STACK = new DrawStack<>();
    private static boolean registered;
    private static boolean leakLogged;
    private static boolean orderLogged;

    private LayerDraws() {}

    public static synchronized void register() {
        if (registered) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new LayerDraws());
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(manager -> {
                Coverage.clear();
                WornCoverage.clear();
            });
        registered = true;
    }

    public static LayerDraw begin(Entity entity, DrawKind kind, float scale) {
        VoxelLayers.sweepIfDue();
        Draw draw = new Draw(entity, kind, scale, voxelsAllowed(entity, kind));
        Draw outer = STACK.peek();
        if (outer != null && outer.entity == entity) {
            draw.suppressed = outer.suppressed;
            draw.gloved = outer.gloved;
        }
        if (outer == null || outer.entity != entity) {
            EarsAnchors.opened(entity);
            draw.seeThrough = TranslucentLayers.opened(entity, kind);
        }
        if (!STACK.push(draw) && !leakLogged) {
            leakLogged = true;
            Salamander.LOG.error("Skin layer draws were opened without being closed; dropped them");
        }
        return draw;
    }

    public static LayerDraw current() {
        return STACK.peek();
    }

    /**
     * The first-person arm drawn in {@code draw} wears the Aether's gloves, which cover its sleeve as they do in the
     * world. Draws opened inside it for the same player inherit this.
     */
    public static void gloved(LayerDraw draw) {
        if (draw instanceof Draw own) own.gloved = true;
    }

    /** Whether the innermost open draw is a first-person arm's, which submits its sleeve after the arm. */
    public static boolean firstPersonOpen() {
        Draw draw = STACK.peek();
        return draw != null && draw.kind == DrawKind.FIRST_PERSON && !draw.suppressed;
    }

    private static boolean voxelsAllowed(Entity entity, DrawKind kind) {
        if (!(entity instanceof AbstractClientPlayer player) || SmartMovingCompat.isLoaded()) return false;
        if (kind != DrawKind.WORLD) return true;
        Entity viewer = Minecraft.getMinecraft().renderViewEntity;
        if (viewer == null || viewer == player) return true;
        double lod = ViewerSkinSettings.lodDistance();
        return player.getDistanceSqToEntity(viewer) < lod * lod;
    }

    /** Helmet covers the hat, chestplate the jacket and sleeves, leggings or boots the pants. */
    public static boolean coveredByArmor(EntityPlayer player, SkinPart part) {
        if (player.inventory == null) return false;
        return switch (part) {
            case HAT -> player.inventory.armorItemInSlot(3) != null;
            case JACKET, LEFT_SLEEVE, RIGHT_SLEEVE -> player.inventory.armorItemInSlot(2) != null;
            case LEFT_PANTS, RIGHT_PANTS -> player.inventory.armorItemInSlot(1) != null
                || player.inventory.armorItemInSlot(0) != null;
            case CAPE -> false;
        };
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                VoxelLayers.clear();
                WornCoverage.clear();
                SkinLayers3DSetup.clearSkullCache();
            });
    }

    private static final class Draw implements LayerDraw, LayerPainter {

        private final Entity entity;
        private final DrawKind kind;
        private final float scale;
        private final boolean voxels;
        private PlayerSkin skin;
        /** Set on the entity's outermost draw. */
        private TranslucentLayers.Open seeThrough;
        private boolean suppressed;
        private boolean gloved;
        /** What the player wears, looked up on the first submit. */
        private WornCoverage worn;
        private final EnumSet<SkinPart> claimed = EnumSet.noneOf(SkinPart.class);
        private boolean closed;

        Draw(Entity entity, DrawKind kind, float scale, boolean voxels) {
            this.entity = entity;
            this.kind = kind;
            this.scale = scale;
            this.voxels = voxels;
        }

        @Override
        public void submit(LayerSegment segment) {
            if (closed) throw new IllegalStateException("Layer draw is already closed");
            // Ears anchors to where each part goes, drawn or not.
            EarsAnchors.submitted(entity, segment);
            if (suppressed) return;

            SkinPart part = segment.part();
            Coverage coverage = Coverage.NONE;
            if (entity instanceof EntityPlayer player) {
                if (!PlayerSkins.isShown(player, part)) return;
                if (worn == null) worn = WornCoverage.of(player);
                coverage = worn.of(part, kind == DrawKind.FIRST_PERSON, gloved);
            }
            // Covered texels are left out, flat or 3D; what is covered whole is not drawn.
            Coverage.Mask mask = coverage.mask(segment);
            if (!mask.none() && ViewerSkinSettings.hideOverlayUnderArmor()) return;
            boolean volumetric = voxels && !mask.unmapped() && ViewerSkinSettings.volumetric(part);
            if (mask.all()) return;
            if (mask.none()) {
                TranslucentLayers.paint(entity, segment, volumetric, this);
                return;
            }
            TranslucentLayers.paint(entity, segment, volumetric, (painted, voxel) -> paint(painted, voxel, mask));
        }

        @Override
        public void paint(LayerSegment segment, boolean volumetric) {
            paint(segment, volumetric, Coverage.Mask.NONE);
        }

        private void paint(LayerSegment segment, boolean volumetric, Coverage.Mask mask) {
            if (volumetric) {
                if (skin == null) skin = PlayerSkins.get((AbstractClientPlayer) entity);
                if (VoxelLayers.draw(skin, segment, scale, mask) != VoxelLayers.Result.UNAVAILABLE) return;
            }
            if (mask.all()) return;
            OverlayBoxes.draw(segment, scale, mask.unmapped() ? Coverage.Mask.NONE : mask);
        }

        @Override
        public void suppress() {
            suppressed = true;
        }

        @Override
        public boolean suppressed() {
            return suppressed;
        }

        @Override
        public void claim(SkinPart part) {
            claimed.add(part);
        }

        @Override
        public boolean claimed(SkinPart part) {
            return claimed.contains(part);
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (!STACK.remove(this) && !orderLogged) {
                orderLogged = true;
                Salamander.LOG.error("Skin layer draws were closed out of order");
            }
            if (seeThrough != null) TranslucentLayers.closed(seeThrough);
        }
    }
}
