package industries.angelic.salamander.mixins.late.aether;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.salamander.api.skin.DrawKind;
import industries.angelic.salamander.api.skin.LayerDraw;
import industries.angelic.salamander.api.skin.SkinLayers;
import industries.angelic.salamander.skin.layers.LayerDraws;

@Pseudo
@Mixin(targets = "com.gildedgames.the_aether.client.renders.AetherItemRenderer", remap = false)
public abstract class MixinAetherItemRenderer {

    /** Over the flat sleeve, as the gloves are in the world. */
    @Unique
    private final ModelBiped salamander$legacyGloveModel = new ModelBiped(0.4F);

    @Redirect(
        method = "renderFirstPersonArm",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderPlayer;renderFirstPersonArm(Lnet/minecraft/entity/player/EntityPlayer;)V",
            ordinal = 1,
            remap = true),
        require = 1,
        remap = false)
    private void salamander$renderGlovedArm(RenderPlayer renderer, EntityPlayer player) {
        if (!(player instanceof AbstractClientPlayer client)) {
            renderer.renderFirstPersonArm(player);
            return;
        }
        // The arm's own sleeve draw opens inside this one and leaves out what the glove covers.
        try (LayerDraw draw = SkinLayers.begin(client, DrawKind.FIRST_PERSON, 0.0625F)) {
            LayerDraws.gloved(draw);
            renderer.renderFirstPersonArm(player);
        }
    }

    @Redirect(
        method = "renderFirstPersonArm",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V", remap = true),
        require = 1,
        remap = false)
    private void salamander$renderLegacyGloveArm(ModelRenderer playerArm, float scale) {
        ModelRenderer gloveArm = this.salamander$legacyGloveModel.bipedRightArm;
        // Slim arms hang lower.
        gloveArm.setRotationPoint(playerArm.rotationPointX, playerArm.rotationPointY, playerArm.rotationPointZ);
        gloveArm.rotateAngleX = playerArm.rotateAngleX;
        gloveArm.rotateAngleY = playerArm.rotateAngleY;
        gloveArm.rotateAngleZ = playerArm.rotateAngleZ;

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0F, -10.0F);
        try {
            gloveArm.render(scale);
        } finally {
            GL11.glPolygonOffset(0.0F, 0.0F);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }
    }
}
