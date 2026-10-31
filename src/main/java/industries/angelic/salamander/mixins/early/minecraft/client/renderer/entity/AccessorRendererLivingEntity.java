package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RendererLivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RendererLivingEntity.class)
public interface AccessorRendererLivingEntity {

    @Accessor("mainModel")
    void salamander$setMainModel(ModelBase model);
}
