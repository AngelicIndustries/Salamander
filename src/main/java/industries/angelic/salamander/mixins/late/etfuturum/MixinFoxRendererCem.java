package industries.angelic.salamander.mixins.late.etfuturum;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import industries.angelic.salamander.cem.client.CemRuntime;

@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.client.renderer.entity.FoxRenderer", remap = false)
public abstract class MixinFoxRendererCem {

    /** Whether the item in the fox's mouth went to the pack model's {@code fox_item} attachment. */
    @Unique
    private boolean salamander$attached;

    /**
     * In place of moving to the native head's pivot: the pack's {@code fox_item} attachment, which also carries the
     * head's turn, or else the pack head's pivot, as modern foxes place it at the animated head.
     */
    @WrapOperation(
        method = { "renderEquippedItems(Lganymedes01/etfuturum/entities/EntityFox;F)V",
            "func_77029_a(Lganymedes01/etfuturum/entities/EntityFox;F)V" },
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", ordinal = 1))
    private void salamander$foxItem(float x, float y, float z, Operation<Void> original) {
        salamander$attached = CemRuntime.attach("fox_item", .0625f);
        if (!salamander$attached && !CemRuntime.pivot("head", .0625f)) original.call(x, y, z);
    }

    /** The native head's yaw and pitch, which the attachment has already applied as the pack animates them. */
    @WrapOperation(
        method = { "renderEquippedItems(Lganymedes01/etfuturum/entities/EntityFox;F)V",
            "func_77029_a(Lganymedes01/etfuturum/entities/EntityFox;F)V" },
        at = { @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 0),
            @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 1) })
    private void salamander$headTurn(float angle, float x, float y, float z, Operation<Void> original) {
        if (!salamander$attached) original.call(angle, x, y, z);
    }
}
