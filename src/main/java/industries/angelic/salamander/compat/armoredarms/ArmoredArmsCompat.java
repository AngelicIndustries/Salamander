package industries.angelic.salamander.compat.armoredarms;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.util.ResourceLocation;

import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.cem.client.CemPlayers;
import industries.angelic.salamander.cem.client.CemRuntime;

/**
 * ArmoredArms draws the first-person arm itself, in place of {@code RenderPlayer.renderFirstPersonArm}, so a CEM
 * player pack's hand would never draw. Its right arm becomes the pack's hand, posed as ArmoredArms poses its own; its
 * armor still goes on top. Reflection only: ArmoredArms is not a build dependency.
 */
public final class ArmoredArmsCompat {

    private static final float SCALE = 0.0625F;
    private static Field renderPlayer;
    private static Field playerSkin;
    private static Field shouldRenderWear;
    private static Method prepareModel;
    private static ModelBiped handModel;
    private static boolean failed;

    private ArmoredArmsCompat() {}

    /** Draws the pack's hand for ArmoredArms' arm. False when ArmoredArms should draw the arm itself. */
    public static boolean cemArm(Object manager, Object side) {
        if (failed || !"RIGHT".equals(((Enum<?>) side).name())) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null) return false;
        try {
            if (renderPlayer == null) {
                Class<?> type = manager.getClass();
                renderPlayer = type.getField("renderPlayer");
                playerSkin = type.getField("playerSkin");
                shouldRenderWear = type.getField("shouldRenderWear");
                prepareModel = type.getMethod("prepareModel", ModelBiped.class);
            }
            RenderPlayer renderer = (RenderPlayer) renderPlayer.get(manager);
            ResourceLocation skin = (ResourceLocation) playerSkin.get(manager);
            if (renderer == null || renderer.modelBipedMain == null || skin == null) return false;
            minecraft.getTextureManager()
                .bindTexture(skin);
            ModelBiped model = prepareHand(manager, prepareModel);
            return CemRuntime.firstPersonArm(model, minecraft.thePlayer, SCALE, shouldRenderWear.getBoolean(manager));
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            Salamander.LOG.error("ArmoredArms support failed; CEM first-person hands are off with ArmoredArms", e);
            return false;
        }
    }

    static ModelBiped prepareHand(Object manager, Method prepare) throws ReflectiveOperationException {
        if (handModel == null) {
            // World renderers can use hierarchical bones (EMB), cached animations and armor-pass visibility.
            // The first-person CEM slot needs a standalone vanilla arm, just like RenderPlayer's hand.
            handModel = new ModelBiped();
            CemPlayers.register(handModel);
        }
        // This also updates AA's mainBones, so its later armor/glove layers use the very same pose.
        // Do not replace renderPlayer.modelBipedMain: it still belongs to the world renderer. If CEM
        // declines the hand, AA's original renderArm prepares that original model and restores its bones.
        prepare.invoke(manager, handModel);
        return handModel;
    }
}
