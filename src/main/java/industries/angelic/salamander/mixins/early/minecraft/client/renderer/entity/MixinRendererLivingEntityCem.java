package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import industries.angelic.salamander.cem.client.CemExporter;
import industries.angelic.salamander.cem.client.CemRuntime;
import industries.angelic.salamander.cem.client.CemTargets;
import industries.angelic.salamander.config.CemConfig;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntityCem {

    /**
     * Other mods' living entities, named {@code <modid>:<entity_name>}, have no model hooks of Salamander's own: their
     * main model draws as a pack model here. Every living entity's main model, vanilla ones too, is offered to the
     * exporter.
     */
    @WrapOperation(
        method = "renderModel(Lnet/minecraft/entity/EntityLivingBase;FFFFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V"))
    private void salamander$moddedModel(ModelBase model, Entity entity, float limb, float speed, float age, float yaw,
        float pitch, float scale, Operation<Void> original) {
        if (CemConfig.exportModels) CemExporter.offer(entity, model);
        if (CemTargets.builtIn(entity) || CemTargets.target(entity) == null) {
            original.call(model, entity, limb, speed, age, yaw, pitch, scale);
            return;
        }
        CemRuntime.Draw previous = CemRuntime.begin(model, entity, limb, speed, age, yaw, pitch);
        try {
            original.call(model, entity, limb, speed, age, yaw, pitch, scale);
        } finally {
            CemRuntime.end(previous, scale);
        }
    }
}
