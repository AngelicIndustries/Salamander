package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.entity.projectile.EntityArrow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.client.CemArrowModel;

@Mixin(RenderArrow.class)
public abstract class MixinRenderArrowCem extends Render {

    @Unique
    private CemArrowModel salamander$arrow;

    @Inject(
        method = "doRender(Lnet/minecraft/entity/projectile/EntityArrow;DDDFF)V",
        at = @At("HEAD"),
        cancellable = true)
    private void salamander$render(EntityArrow arrow, double x, double y, double z, float yaw, float partial,
        CallbackInfo ci) {
        if (salamander$arrow == null) salamander$arrow = new CemArrowModel();
        bindEntityTexture(arrow);
        if (salamander$arrow.renderArrow(arrow, x, y, z, partial)) ci.cancel();
    }
}
