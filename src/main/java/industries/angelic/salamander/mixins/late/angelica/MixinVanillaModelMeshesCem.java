package industries.angelic.salamander.mixins.late.angelica;

import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.model.ModelSkeletonHead;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.salamander.cem.client.CemRuntime;

/**
 * Angelica draws vanilla chests, signs and skulls from cached meshes instead of calling the model, so a pack model for
 * one of them never draws, nor its texture's emissive mask. Those get the model's own render call back.
 */
@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.tesr.VanillaModelMeshes", remap = false)
public abstract class MixinVanillaModelMeshesCem {

    @Inject(method = "renderChest", at = @At("HEAD"), cancellable = true)
    private static void salamander$chest(ModelChest model, ResourceLocation texture, boolean isDouble,
        CallbackInfo ci) {
        if (!CemRuntime.claims(model) && !CemRuntime.glows(texture)) return;
        model.renderAll();
        ci.cancel();
    }

    @Inject(method = "renderSign", at = @At("HEAD"), cancellable = true)
    private static void salamander$sign(ModelSign model, CallbackInfo ci) {
        if (!CemRuntime.claims(model) && !CemRuntime.glows(CemRuntime.texture())) return;
        model.renderSign();
        ci.cancel();
    }

    /** False makes Angelica call the model's render itself. */
    @Inject(method = "renderSkull", at = @At("HEAD"), cancellable = true)
    private static void salamander$skull(ModelSkeletonHead model, ResourceLocation texture, float yaw, float pitch,
        float scale, CallbackInfoReturnable<Boolean> cir) {
        if (CemRuntime.claims(model) || CemRuntime.glows(texture)) cir.setReturnValue(false);
    }
}
