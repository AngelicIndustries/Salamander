package industries.angelic.salamander.mixins.early.minecraft.client.renderer;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemBeds;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobalCemBeds {

    @org.spongepowered.asm.mixin.Shadow
    private int renderEntitiesStartupCounter;

    @WrapMethod(method = "renderEntities")
    private void salamander$beds(EntityLivingBase camera, ICamera frustum, float partial, Operation<Void> original) {
        boolean ready = renderEntitiesStartupCounter <= 0;
        original.call(camera, frustum, partial);
        if (ready) CemBeds.render(camera, frustum, partial);
    }
}
