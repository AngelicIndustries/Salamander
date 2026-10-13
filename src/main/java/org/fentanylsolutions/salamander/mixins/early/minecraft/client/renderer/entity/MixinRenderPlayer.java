package org.fentanylsolutions.salamander.mixins.early.minecraft.client.renderer.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.salamander.cem.client.CemPlayers;
import org.fentanylsolutions.salamander.cem.client.CemRuntime;
import org.fentanylsolutions.salamander.mixins.early.minecraft.client.AccessorMinecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.geckolib.renderer.GeoArmorRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/** Scopes player CEM rendering and adds registered GeckoLib armor to the first-person arm. */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer {

    @Shadow
    public ModelBiped modelBipedMain;

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At("HEAD"))
    private void salamander$playerModel(net.minecraft.client.entity.AbstractClientPlayer player, double x, double y,
        double z, float yaw, float partial, CallbackInfo ci) {
        CemPlayers.register(modelBipedMain);
    }

    @WrapOperation(
        method = "renderFirstPersonArm",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V"))
    private void salamander$cemHand(ModelRenderer arm, float scale, Operation<Void> original, EntityPlayer player) {
        CemPlayers.register(modelBipedMain);
        if (!CemRuntime.firstPersonArm(modelBipedMain, player, scale)) original.call(arm, scale);
    }

    @WrapOperation(
        method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBiped;renderCloak(F)V"))
    private void salamander$cemCape(ModelBiped model, float scale, Operation<Void> original) {
        CemRuntime.playerCape(model, scale, () -> original.call(model, scale));
    }

    @Inject(method = "renderFirstPersonArm", at = @At("TAIL"))
    private void salamander$renderFirstPersonArmor(EntityPlayer player, CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getMinecraft();

        if (player != minecraft.thePlayer) return;

        GeoArmorRenderer
            .renderFirstPersonArm(player, ((AccessorMinecraft) minecraft).salamander$getTimer().renderPartialTicks);
    }
}
