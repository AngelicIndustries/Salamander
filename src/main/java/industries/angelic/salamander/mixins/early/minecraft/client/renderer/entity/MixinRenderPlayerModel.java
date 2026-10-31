package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.api.skin.DrawKind;
import industries.angelic.salamander.api.skin.LayerDraw;
import industries.angelic.salamander.api.skin.PlayerSkins;
import industries.angelic.salamander.api.skin.SkinLayers;
import industries.angelic.salamander.compat.avaritia.AvaritiaOverlay;
import industries.angelic.salamander.compat.ears.EarsDetect;
import industries.angelic.salamander.compat.smartmoving.SmartRenderModels;
import industries.angelic.salamander.compat.wallgrab.WallGrabCompat;
import industries.angelic.salamander.skin.model.ModelPlayer;

/**
 * Installs {@link ModelPlayer} in the vanilla player renderer and keeps it on each drawn player's skin model, and in
 * the renderer when Wall Grab swaps in its own biped.
 */
@Mixin(RenderPlayer.class)
public class MixinRenderPlayerModel {

    @Shadow
    public ModelBiped modelBipedMain;

    /**
     * The constructor's first model becomes both {@code mainModel} and {@code modelBipedMain}. Subclasses that install
     * their own model afterwards keep it. With Ears active, Ears owns the player model.
     */
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/client/model/ModelBiped", ordinal = 0))
    private static ModelBiped salamander$installModelPlayer(float scale) {
        if (EarsDetect.isRendererActive()) {
            return new ModelBiped(scale);
        }
        return new ModelPlayer(scale);
    }

    /** The model the constructor installed, put back when Wall Grab swaps in its own. */
    @Unique
    private ModelPlayer salamander$model;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void salamander$rememberModel(CallbackInfo ci) {
        if (this.modelBipedMain instanceof ModelPlayer model) this.salamander$model = model;
    }

    @Unique
    private void salamander$keepModel() {
        if (this.salamander$model == null || !WallGrabCompat.replaced(this.modelBipedMain)) return;
        this.modelBipedMain = this.salamander$model;
        ((AccessorRendererLivingEntity) this).salamander$setMainModel(this.salamander$model);
    }

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At("HEAD"))
    private void salamander$applySkinModel(AbstractClientPlayer player, double x, double y, double z, float yaw,
        float partialTicks, CallbackInfo ci) {
        this.salamander$keepModel();
        if (this.modelBipedMain instanceof ModelPlayer model) {
            model.setSlim(PlayerSkins.slim(player));
        } else {
            SmartRenderModels.prepare(this.modelBipedMain, player);
        }
    }

    /**
     * The first-person arm gets its sleeve; the draw stays open around the arm so the arm's renderers can suppress it.
     */
    @WrapMethod(method = "renderFirstPersonArm")
    private void salamander$firstPersonSleeve(EntityPlayer player, Operation<Void> original) {
        this.salamander$keepModel();
        if (!(this.modelBipedMain instanceof ModelPlayer model) || !(player instanceof AbstractClientPlayer client)) {
            SmartRenderModels.prepare(this.modelBipedMain, player);
            original.call(player);
            return;
        }
        model.setSlim(PlayerSkins.slim(client));
        try (LayerDraw draw = SkinLayers.begin(client, DrawKind.FIRST_PERSON, 0.0625F)) {
            original.call(player);
            if (!draw.suppressed()) model.drawFirstPersonSleeve(draw, 0.0625F);
        }
        // ArmoredArms can draw another arm pass inside original; its early arm hook is too soon for the shimmer.
        AvaritiaOverlay.firstPerson(player, model.bipedRightArm, true);
    }
}
