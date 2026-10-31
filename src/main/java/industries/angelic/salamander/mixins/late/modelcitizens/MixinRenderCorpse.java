package industries.angelic.salamander.mixins.late.modelcitizens;

import net.minecraft.client.model.ModelBiped;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import industries.angelic.salamander.api.skin.DrawKind;
import industries.angelic.salamander.api.skin.LayerDraw;
import industries.angelic.salamander.api.skin.SkinLayers;
import industries.angelic.salamander.skin.model.ModelPlayer;

/**
 * Model Citizens draws its figures with a vanilla 64x32 model, part by part. A figure wearing a 64x64 skin gets Wawel
 * Auth's player model instead, chosen from the bound texture's shape, and its overlay layers after the last part.
 * Figures aren't players, so the layers are flat; the vanilla headwear part already drew the hat.
 */
@Pseudo
@Mixin(targets = "net.fybertech.modelcitizens.RenderCorpse", remap = false)
public abstract class MixinRenderCorpse {

    @Unique
    private static final float salamander$SCALE = 0.0625F;

    @Shadow(remap = false)
    public ModelBiped modelMain;

    @Unique
    private ModelBiped salamander$legacyModel;

    @Unique
    private final ModelPlayer salamander$modernModel = new ModelPlayer(0.0F);

    /** Right after the figure's skin is bound and before the model is posed. */
    @Inject(
        method = "func_147500_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fybertech/modelcitizens/RenderCorpse;func_147499_a(Lnet/minecraft/util/ResourceLocation;)V",
            ordinal = 0,
            shift = At.Shift.AFTER),
        remap = false)
    private void salamander$pickModel(CallbackInfo ci) {
        if (this.salamander$legacyModel == null) this.salamander$legacyModel = this.modelMain;
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        this.modelMain = width > 0 && width == height ? this.salamander$modernModel : this.salamander$legacyModel;
    }

    /** After the seventh part, the headwear. */
    @Inject(
        method = "func_147500_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V",
            ordinal = 6,
            shift = At.Shift.AFTER,
            remap = true),
        remap = false)
    private void salamander$layers(CallbackInfo ci, @Local ModelBiped drawn) {
        if (drawn != this.salamander$modernModel) return;
        try (LayerDraw draw = SkinLayers.begin(null, DrawKind.WORLD, salamander$SCALE)) {
            this.salamander$modernModel.drawLayers(draw, salamander$SCALE, false);
        }
    }
}
