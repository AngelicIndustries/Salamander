package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderSpider;
import net.minecraft.entity.monster.EntitySpider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.salamander.cem.client.CemResources;

/** Rebinds after vanilla/Angelica setup so their full-bright and shader pass hooks still execute. */
@Mixin(RenderSpider.class)
public abstract class MixinRenderSpiderCem extends RenderLiving {

    protected MixinRenderSpiderCem(ModelBase model, float shadow) {
        super(model, shadow);
    }

    @Inject(
        method = "shouldRenderPass(Lnet/minecraft/entity/monster/EntitySpider;IF)I",
        at = @At("HEAD"),
        cancellable = true)
    private void salamander$hideInvisibleEyes(EntitySpider entity, int pass, float partialTicks,
        CallbackInfoReturnable<Integer> cir) {
        if (industries.angelic.salamander.cem.client.CemRuntime.active() != null && entity.isInvisible())
            cir.setReturnValue(-1);
    }

    @Inject(method = "shouldRenderPass(Lnet/minecraft/entity/monster/EntitySpider;IF)I", at = @At("RETURN"))
    private void salamander$emissiveEyes(EntitySpider entity, int pass, float partialTicks,
        CallbackInfoReturnable<Integer> cir) {
        if (industries.angelic.salamander.cem.client.CemRuntime.active() == null || cir.getReturnValue() <= 0) return;
        CemResources.Entry entry = industries.angelic.salamander.cem.client.CemRuntime.active();
        net.minecraft.util.ResourceLocation mask = entry == null ? null : CemResources.INSTANCE.emissive(entry.texture);
        if (mask != null) bindTexture(mask);
    }
}
