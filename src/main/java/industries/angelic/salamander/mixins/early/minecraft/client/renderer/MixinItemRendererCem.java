package industries.angelic.salamander.mixins.early.minecraft.client.renderer;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemItemContext;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRendererCem {

    @WrapMethod(
        method = "renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V",
        remap = false)
    private void salamander$hand(EntityLivingBase entity, ItemStack stack, int pass, ItemRenderType type,
        Operation<Void> original) {
        int previous = CemItemContext.enter(1);
        try {
            original.call(entity, stack, pass, type);
        } finally {
            CemItemContext.leave(previous);
        }
    }
}
