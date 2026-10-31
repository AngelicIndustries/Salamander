package industries.angelic.salamander.compat.avaritia;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.api.skin.PlayerSkins;

/**
 * Avaritia's full infinity set draws a cosmic shimmer over the wearer's whole body, on a plain biped the size of the
 * skin. Its bare arms fight the skin for depth, on slim arms it also sticks out and stops half a pixel short, and
 * nothing draws it on the first-person arm. Its arms are fitted to the wearer's, a hair outside the skin, and the
 * first-person arm gets it too. Read by reflection; nothing changes when Avaritia
 * is not loaded or has changed.
 */
@SideOnly(Side.CLIENT)
public final class AvaritiaOverlay {

    private static final String RENDER = "fox.spiteful.avaritia.render.";
    private static final boolean LOADED = Loader.isModLoaded("Avaritia");
    private static final float SCALE = 0.0625F;
    /** Each shimmer's own arms and their slim stand-ins, by the shimmer. */
    private static final Map<ModelBiped, Arms> ARMS = new WeakHashMap<>();

    private static boolean resolved;
    private static boolean failed;
    private static Field overlay;
    private static Field showing;
    private static Field textureX;
    private static Field textureY;
    private static Method useShader;
    private static Method bindTexture;
    private static Method releaseShader;

    private AvaritiaOverlay() {}

    /** After Avaritia readies {@code armor} for {@code wearer}: its shimmer's arms match the wearer's. */
    public static void fit(ModelBiped armor, EntityLivingBase wearer) {
        if (!resolve()) return;
        try {
            ModelBiped shimmer = (ModelBiped) overlay.get(armor);
            if (shimmer == null) return;
            Arms arms = ARMS.get(shimmer);
            ModelRenderer right = shimmer.bipedRightArm, left = shimmer.bipedLeftArm;
            // Avaritia builds the arms again when textures reload
            if (arms == null || !arms.owns(right, left)) ARMS.put(shimmer, arms = new Arms(shimmer, right, left));
            boolean slim = wearer instanceof AbstractClientPlayer && PlayerSkins.slim((AbstractClientPlayer) wearer);
            shimmer.bipedRightArm = slim ? arms.slimRight : arms.wideRight;
            shimmer.bipedLeftArm = slim ? arms.slimLeft : arms.wideLeft;
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
        }
    }

    /** Draws the shimmer after the first-person arm and its armor layers have finished. */
    public static void firstPerson(EntityPlayer player, ModelRenderer arm, boolean right) {
        if (!resolve() || arm == null || arm.isHidden || !arm.showModel) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        // Only the camera's own arm in first person: other renderers may draw an arm in the world's frame
        if (minecraft.gameSettings.thirdPersonView != 0 || player != minecraft.renderViewEntity) return;
        ItemStack helmet = player.getEquipmentInSlot(4);
        if (helmet == null || helmet.getItem() == null) return;
        try {
            ModelBiped armor = helmet.getItem()
                .getArmorModel(player, helmet, 0);
            if (armor == null || !armor.getClass()
                .getName()
                .equals(RENDER + "ModelArmorInfinity") || !showing.getBoolean(armor)) return;
            ModelBiped shimmer = (ModelBiped) overlay.get(armor);
            ModelRenderer part = shimmer == null ? null : right ? shimmer.bipedRightArm : shimmer.bipedLeftArm;
            if (part == null) return;
            draw(part, arm);
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
        }
    }

    /** {@code part} where {@code arm} was drawn, as Avaritia draws the shimmer: through its cosmic shader, faintly. */
    private static void draw(ModelRenderer part, ModelRenderer arm) throws ReflectiveOperationException {
        float x = part.rotationPointX, y = part.rotationPointY, z = part.rotationPointZ;
        float ax = part.rotateAngleX, ay = part.rotateAngleY, az = part.rotateAngleZ;
        part.rotationPointX = arm.rotationPointX;
        part.rotationPointY = arm.rotationPointY;
        part.rotationPointZ = arm.rotationPointZ;
        part.rotateAngleX = arm.rotateAngleX;
        part.rotateAngleY = arm.rotateAngleY;
        part.rotateAngleZ = arm.rotateAngleZ;
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            useShader.invoke(null);
            bindTexture.invoke(null);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDepthMask(false);
            GL11.glColor4d(1.0D, 1.0D, 1.0D, 0.2D);
            part.render(SCALE);
        } finally {
            releaseShader.invoke(null);
            GL11.glPopAttrib();
            part.rotationPointX = x;
            part.rotationPointY = y;
            part.rotationPointZ = z;
            part.rotateAngleX = ax;
            part.rotateAngleY = ay;
            part.rotateAngleZ = az;
        }
    }

    private static boolean resolve() {
        if (!LOADED || failed) return false;
        if (resolved) return true;
        try {
            Class<?> model = Class.forName(RENDER + "ModelArmorInfinity");
            overlay = model.getDeclaredField("invulnOverlay");
            overlay.setAccessible(true);
            showing = model.getDeclaredField("invulnRender");
            showing.setAccessible(true);
            Class<?> shaders = Class.forName(RENDER + "CosmicRenderShenanigans");
            useShader = shaders.getMethod("useShader");
            bindTexture = shaders.getMethod("bindItemTexture");
            releaseShader = shaders.getMethod("releaseShader");
            textureX = field(ModelRenderer.class, "field_78803_o", "textureOffsetX");
            textureY = field(ModelRenderer.class, "field_78813_p", "textureOffsetY");
            resolved = true;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return false;
        }
    }

    private static Field field(Class<?> type, String obfuscated, String named) throws NoSuchFieldException {
        Field field;
        try {
            field = type.getDeclaredField(obfuscated);
        } catch (NoSuchFieldException e) {
            field = type.getDeclaredField(named);
        }
        field.setAccessible(true);
        return field;
    }

    private static void fail(Exception e) {
        failed = true;
        Salamander.LOG.error("Avaritia infinity shimmer support failed; it stays as Avaritia draws it", e);
    }

    /**
     * A shimmer's arms as Avaritia built them, and stand-ins from the same texture spot sized like the skin's, wide or
     * slim: a hair outside the skin, which Avaritia's skin-tight arms fight for depth, and inside the sleeve layer.
     */
    private static final class Arms {

        /** How far the stand-ins sit out from the skin, in pixels; the flat sleeve layer sits a quarter out. */
        private static final float OUT = 0.05F;

        final ModelRenderer right, left, wideRight, wideLeft, slimRight, slimLeft;

        Arms(ModelBiped shimmer, ModelRenderer right, ModelRenderer left) throws IllegalAccessException {
            this.right = right;
            this.left = left;
            this.wideRight = arm(shimmer, right, -3.0F, 4, -5.0F, 2.0F);
            this.wideLeft = arm(shimmer, left, -1.0F, 4, 5.0F, 2.0F);
            this.slimRight = arm(shimmer, right, -2.0F, 3, -5.0F, 2.5F);
            this.slimLeft = arm(shimmer, left, -1.0F, 3, 5.0F, 2.5F);
        }

        boolean owns(ModelRenderer right, ModelRenderer left) {
            return (right == this.right || right == this.wideRight || right == this.slimRight)
                && (left == this.left || left == this.wideLeft || left == this.slimLeft);
        }

        /** As the skin's arm, {@code width} pixels wide from {@code x}, on its pivot. */
        private static ModelRenderer arm(ModelBiped shimmer, ModelRenderer arm, float x, int width, float pivotX,
            float pivotY) throws IllegalAccessException {
            ModelRenderer part = new ModelRenderer(shimmer, textureX.getInt(arm), textureY.getInt(arm));
            shimmer.boxList.remove(part);
            part.mirror = arm.mirror;
            part.addBox(x, -2.0F, -2.0F, width, 12, 4, OUT);
            part.setRotationPoint(pivotX, pivotY, 0.0F);
            return part;
        }
    }
}
