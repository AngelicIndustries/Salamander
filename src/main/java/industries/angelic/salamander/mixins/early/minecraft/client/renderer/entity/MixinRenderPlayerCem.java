package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import industries.angelic.salamander.cem.client.CemArmor;
import industries.angelic.salamander.cem.client.CemRuntime;

@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayerCem {

    @WrapOperation(
        method = "shouldRenderPass(Lnet/minecraft/client/entity/AbstractClientPlayer;IF)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/ForgeHooksClient;getArmorModel(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;ILnet/minecraft/client/model/ModelBiped;)Lnet/minecraft/client/model/ModelBiped;",
            remap = false))
    private ModelBiped salamander$armorModel(EntityLivingBase entity, ItemStack stack, int slot, ModelBiped model,
        Operation<ModelBiped> original) {
        ModelBiped used = original.call(entity, stack, slot, model);
        CemArmor.offer(model, slot, used);
        return used;
    }

    /** A pumpkin or skull worn on the head goes to the pack model's {@code head_item} attachment. */
    @WrapOperation(
        method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;postRender(F)V", ordinal = 0))
    private void salamander$headItem(ModelRenderer head, float scale, Operation<Void> original) {
        if (!CemRuntime.attach("head_item", scale)) original.call(head, scale);
    }
}
