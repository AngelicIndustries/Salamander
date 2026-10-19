package industries.angelic.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemItemContext;

@Mixin(RenderItem.class)
public abstract class MixinRenderItemCem {

    @WrapMethod(
        method = "renderItemIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IIZ)V",
        remap = false)
    private void salamander$gui(FontRenderer font, TextureManager textures, ItemStack stack, int x, int y,
        boolean effect, Operation<Void> original) {
        int previous = CemItemContext.enter(2);
        try {
            original.call(font, textures, stack, x, y, effect);
        } finally {
            CemItemContext.leave(previous);
        }
    }
}
