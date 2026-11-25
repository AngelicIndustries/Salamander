package industries.angelic.salamander.mixins.early.minecraft.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.client.CemPartCalls;
import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(ModelRenderer.class)
public abstract class MixinModelRendererCem implements CemPartCalls {

    @Shadow
    private ModelBase baseModel;
    @Shadow
    private int displayList;

    @Unique
    private int salamander$callStamp;
    @Unique
    private int salamander$calls;

    @Override
    public int salamander$nextCall(int stamp) {
        if (salamander$callStamp != stamp) {
            salamander$callStamp = stamp;
            salamander$calls = 0;
        }
        return salamander$calls++;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void salamander$render(float scale, CallbackInfo ci) {
        if (CemRuntime.part(baseModel, (ModelRenderer) (Object) this, scale, false)) ci.cancel();
    }

    @Inject(method = "renderWithRotation", at = @At("HEAD"), cancellable = true)
    private void salamander$rotated(float scale, CallbackInfo ci) {
        if (CemRuntime.part(baseModel, (ModelRenderer) (Object) this, scale, true)) ci.cancel();
    }

    /**
     * After the part's display list draws, whoever drew it: Angelica redirects that call to batch parts, and this
     * applies first, so it stays after the redirect.
     */
    @Inject(
        method = { "render", "renderWithRotation" },
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glCallList(I)V",
            shift = At.Shift.AFTER,
            remap = false))
    private void salamander$emissive(float scale, CallbackInfo ci) {
        CemRuntime.emissivePart((ModelRenderer) (Object) this, displayList, scale);
    }

    @Inject(method = "postRender", at = @At("HEAD"), cancellable = true)
    private void salamander$post(float scale, CallbackInfo ci) {
        if (CemRuntime.post(baseModel, (ModelRenderer) (Object) this, scale)) ci.cancel();
    }
}
