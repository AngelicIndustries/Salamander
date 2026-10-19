package com.geckolib.renderer;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.state.ModelPose;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.ItemRenderPerspective;
import com.geckolib.loading.math.MolangContext;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.layer.GeoRenderLayersContainer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.mixins.early.minecraft.client.AccessorMinecraft;

/** Forge 1.7 item renderer for singleton GeckoLib animatables. */
@SideOnly(Side.CLIENT)
public class GeoItemRenderer<T extends Item & GeoItem> implements GeoRenderer<T>, IItemRenderer {

    protected final GeoModel<T> model;
    protected final GeoRenderLayersContainer<T> renderLayers = new GeoRenderLayersContainer<>(this);
    protected float scaleWidth = 1;
    protected float scaleHeight = 1;

    private ItemStack activeStack;
    private ItemRenderPerspective activePerspective;

    public GeoItemRenderer(GeoModel<T> model) {
        this.model = model;
    }

    @Override
    public GeoModel<T> getGeoModel() {
        return this.model;
    }

    @Override
    public List<GeoRenderLayer<T>> getRenderLayers() {
        return this.renderLayers.getRenderLayers();
    }

    public GeoItemRenderer<T> addRenderLayer(GeoRenderLayer<T> renderLayer) {
        this.renderLayers.addLayer(renderLayer);

        return this;
    }

    public boolean removeRenderLayer(GeoRenderLayer<T> renderLayer) {
        return this.renderLayers.removeLayer(renderLayer);
    }

    public GeoItemRenderer<T> withScale(float scale) {
        return withScale(scale, scale);
    }

    public GeoItemRenderer<T> withScale(float widthScale, float heightScale) {
        this.scaleWidth = widthScale;
        this.scaleHeight = heightScale;

        return this;
    }

    /** Unique animation identity for the rendered stack. */
    public long getInstanceId(ItemStack stack) {
        return GeoItem.getId(stack);
    }

    /** ARGB model tint. */
    public int getRenderColor(T animatable, ItemStack stack, ItemRenderPerspective perspective, float partialTicks) {
        return 0xFFFFFFFF;
    }

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public final void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        if (stack == null || !(stack.getItem() instanceof GeoItem)) return;

        T animatable = cast(stack.getItem());
        ItemRenderPerspective perspective = toPerspective(type);
        Minecraft minecraft = Minecraft.getMinecraft();
        float partialTicks = ((AccessorMinecraft) minecraft).salamander$getTimer().renderPartialTicks;
        double animatableAge = minecraft.theWorld == null ? 0 : minecraft.theWorld.getTotalWorldTime() + partialTicks;
        long instanceId = getInstanceId(stack);
        AnimatableManager<T> manager = animatable.getAnimatableInstanceCache()
            .getManagerForId(instanceId);
        int color = getRenderColor(animatable, stack, perspective, partialTicks);

        manager.setAnimatableData(DataTickets.ITEM_STACK, stack);
        manager.setAnimatableData(DataTickets.ITEM_RENDER_PERSPECTIVE, perspective);
        manager.setAnimatableData(DataTickets.RENDER_COLOR, color);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        try {
            this.activeStack = stack;
            this.activePerspective = perspective;
            applyRenderTransform(animatable, stack, perspective, partialTicks);
            GL11.glScalef(this.scaleWidth, this.scaleHeight, this.scaleWidth);
            GeoRenderer.super.render(
                animatable,
                instanceId,
                animatableAge,
                partialTicks,
                createMolangContext(animatable, stack, perspective, partialTicks),
                channel(color, 16),
                channel(color, 8),
                channel(color, 0),
                channel(color, 24));
        } finally {
            this.activeStack = null;
            this.activePerspective = null;
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
        }
    }

    /** Applies the legacy Forge item-space transform before the model is posed and rendered. */
    protected void applyRenderTransform(T animatable, ItemStack stack, ItemRenderPerspective perspective,
        float partialTicks) {
        if (perspective == ItemRenderPerspective.INVENTORY) {
            GL11.glTranslatef(-1, -1, 0);
            GL11.glRotatef(90, 0, 1, 0);
        }

        if (perspective != ItemRenderPerspective.EQUIPPED_FIRST_PERSON) GL11.glTranslatef(0, -0.5f, 0);

        GL11.glTranslatef(0, 0.01f, 0);
        GL11.glTranslatef(0.5f, 0.5f, 0.5f);
        GL11.glRotatef(90, 0, 1, 0);
    }

    protected MolangContext createMolangContext(T animatable, ItemStack stack, ItemRenderPerspective perspective,
        float partialTicks) {
        return MolangContext.EMPTY;
    }

    /** Item-aware model-pose hook, called after the model's own custom animation hook. */
    protected void applyRenderPose(T animatable, ItemStack stack, ItemRenderPerspective perspective, ModelPose pose,
        float partialTicks) {}

    @Override
    public void adjustModelPose(T animatable, ModelPose pose, float partialTicks) {
        if (this.activeStack != null && this.activePerspective != null)
            applyRenderPose(animatable, this.activeStack, this.activePerspective, pose, partialTicks);
    }

    private static ItemRenderPerspective toPerspective(ItemRenderType type) {
        switch (type) {
            case ENTITY:
                return ItemRenderPerspective.ENTITY;
            case EQUIPPED:
                return ItemRenderPerspective.EQUIPPED;
            case EQUIPPED_FIRST_PERSON:
                return ItemRenderPerspective.EQUIPPED_FIRST_PERSON;
            case INVENTORY:
            default:
                return ItemRenderPerspective.INVENTORY;
        }
    }

    private static float channel(int color, int shift) {
        return (color >> shift & 0xFF) / 255f;
    }

    @SuppressWarnings("unchecked")
    private T cast(Item item) {
        return (T) item;
    }
}
