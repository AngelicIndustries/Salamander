package industries.angelic.salamander.mixins.late.aether;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import industries.angelic.salamander.skin.model.ModelPlayer;

/** The accessories the Aether draws after the player, from its own vanilla model. */
@Pseudo
@Mixin(targets = "com.gildedgames.the_aether.client.renders.entity.PlayerAetherRenderer", remap = false)
public abstract class MixinPlayerAetherRenderer {

    @Shadow
    @Final
    public ModelBiped modelMisc;

    /** Mods such as WaveyCapes cancel every renderCloak to draw their own cape, which takes the Aether's with it. */
    @Redirect(
        method = "renderCape",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBiped;renderCloak(F)V", remap = true),
        require = 1)
    private void salamander$drawCape(ModelBiped model, float scale) {
        model.bipedCloak.render(scale);
    }

    /**
     * Gloves and the shield are drawn on the arms of the Aether's vanilla model: they hang where this player's arms do,
     * slim arms lower. The Aether shrinks the gloves towards the middle of the body, so a chestplate's sleeves show
     * through them: they keep their size, and are drawn over a sleeve as big as they are.
     */
    @Inject(method = "renderGloves", at = @At("HEAD"), require = 1)
    private void salamander$glovesOverSleeves(ItemStack gloves, EntityPlayer player, float scale, CallbackInfo ci) {
        GL11.glPushMatrix();
        GL11.glScalef(1.0F / 0.9375F, 1.0F / 0.9375F, 1.0F / 0.9375F);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0F, -10.0F);
        Render render = RenderManager.instance.getEntityRenderObject(player);
        if (!(render instanceof RenderPlayer renderer) || !(renderer.modelBipedMain instanceof ModelPlayer model)) {
            return;
        }
        salamander$pivot(model.bipedRightArm, this.modelMisc.bipedRightArm);
        salamander$pivot(model.bipedLeftArm, this.modelMisc.bipedLeftArm);
    }

    @Inject(method = "renderGloves", at = @At("RETURN"), require = 1)
    private void salamander$glovesDrawn(ItemStack gloves, EntityPlayer player, float scale, CallbackInfo ci) {
        GL11.glPolygonOffset(0.0F, 0.0F);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPopMatrix();
    }

    /**
     * The shield is see-through, but drawn before the wings and boots and before see-through skin layers: writing
     * depth, it would hide them.
     */
    @Redirect(
        method = "renderShield",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelBiped;render(Lnet/minecraft/entity/Entity;FFFFFF)V",
            remap = true),
        require = 1)
    private void salamander$drawShield(ModelBiped model, Entity entity, float limbSwing, float limbSwingAmount,
        float age, float yaw, float pitch, float scale) {
        GL11.glDepthMask(false);
        try {
            model.render(entity, limbSwing, limbSwingAmount, age, yaw, pitch, scale);
        } finally {
            GL11.glDepthMask(true);
        }
    }

    @Unique
    private static void salamander$pivot(ModelRenderer from, ModelRenderer to) {
        to.setRotationPoint(from.rotationPointX, from.rotationPointY, from.rotationPointZ);
    }

    /**
     * The Aether places agility boots as it places the halo, turned with the head, so they slide around the legs as the
     * player looks around. They are placed as for a head looking straight ahead.
     */
    // Departure's source draws them in renderModel; its 26w35a release in renderInExistingSpace.
    @WrapWithCondition(
        method = { "renderModel", "renderInExistingSpace" },
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V"),
        slice = @Slice(
            to = @At(
                value = "INVOKE",
                target = "Lcom/gildedgames/the_aether/client/models/attachments/ModelAgilityBoots;renderWings(F)V")),
        require = 4,
        allow = 4)
    private boolean salamander$bootsFollowTheLegs(float angle, float x, float y, float z) {
        return false;
    }
}
