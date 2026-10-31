package industries.angelic.salamander.mixins.early.minecraft.client.renderer.tileentity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSkeletonHead;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;

import industries.angelic.salamander.api.skin.PlayerSkins;
import industries.angelic.salamander.compat.ssb.SsbTakeover;
import industries.angelic.salamander.config.SkinLayers3DConfig;
import industries.angelic.salamander.config.SkullConfig;
import industries.angelic.salamander.skin.skull.SkinLayers3DSetup;
import industries.angelic.salamander.skin.voxel.VoxelMesh;

/**
 * Skull skin resolution + 3D hat layer.
 * Uses HEAD to capture args, ModifyVariable for model swap,
 * RETURN to re-render with resolved texture + 3D hat.
 */
@Mixin(TileEntitySkullRenderer.class)
public class MixinTileEntitySkullRendererSkin {

    @Shadow
    private ModelSkeletonHead field_147538_h;

    /** Swap to 64x64 UV model for player skulls when modern skin support is on. */
    @ModifyVariable(method = "func_152674_a", at = @At("STORE"), ordinal = 0)
    private ModelSkeletonHead salamander$replaceSkullModel(ModelSkeletonHead original,
        @Local(index = 6, argsOnly = true) int skullType) {
        if (skullType == 3) {
            return field_147538_h;
        }
        return original;
    }

    /** SimpleSkinBackport's own hat on player heads stays hidden while Salamander draws a 3D one. */
    @Inject(method = "func_152674_a", at = @At("HEAD"))
    private void salamander$oneHat(float x, float y, float z, int facing, float rotation, int skullType,
        GameProfile profile, CallbackInfo ci) {
        ModelRenderer hat = SsbTakeover.headHat(field_147538_h);
        if (hat != null) hat.showModel = !(SkinLayers3DConfig.enabled3D && SkullConfig.enableSkulls);
    }

    /** After vanilla renders (with Steve), re-render with our resolved skin + 3D hat. */
    @Inject(method = "func_152674_a", at = @At("RETURN"))
    private void salamander$renderWithResolvedSkin(float x, float y, float z, int facing, float rotation, int skullType,
        GameProfile profile, CallbackInfo ci) {
        if (skullType != 3 || profile == null) return;

        ResourceLocation skin = salamander$resolveSkin(profile);
        if (skin == null) return;

        ModelSkeletonHead model = field_147538_h;

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(skin);

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);

        if (facing != 1) {
            switch (facing) {
                case 2:
                    GL11.glTranslatef(x + 0.5F, y + 0.25F, z + 0.74F);
                    break;
                case 3:
                    GL11.glTranslatef(x + 0.5F, y + 0.25F, z + 0.26F);
                    rotation = 180.0F;
                    break;
                case 4:
                    GL11.glTranslatef(x + 0.74F, y + 0.25F, z + 0.5F);
                    rotation = 270.0F;
                    break;
                case 5:
                default:
                    GL11.glTranslatef(x + 0.26F, y + 0.25F, z + 0.5F);
                    rotation = 90.0F;
                    break;
            }
        } else {
            GL11.glTranslatef(x + 0.5F, y, z + 0.5F);
        }

        float scale = 0.0625F;
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glScalef(-1.0F, -1.0F, 1.0F);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        model.render(null, 0.0F, 0.0F, 0.0F, rotation, 0.0F, scale);

        // 3D hat overlay
        if (SkinLayers3DConfig.enabled3D && SkullConfig.enableSkulls) {
            VoxelMesh hatMesh = SkinLayers3DSetup.getOrCreateSkullMesh(skin);
            if (hatMesh != null) {
                float voxelSize = SkullConfig.skullVoxelSize;
                GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                hatMesh.setPosition(0, 0, 0);
                hatMesh.setRotation(
                    model.skeletonHead.rotateAngleX,
                    model.skeletonHead.rotateAngleY,
                    model.skeletonHead.rotateAngleZ);
                hatMesh.render(scale, voxelSize);

                GL11.glPopAttrib();
            }
        }

        GL11.glPopMatrix();
    }

    @Unique
    private static ResourceLocation salamander$resolveSkin(GameProfile profile) {
        return profile == null ? null : PlayerSkins.skull(profile);
    }
}
