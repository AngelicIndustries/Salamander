package industries.angelic.salamander.mixins.early.minecraft.client.resources;

import java.util.List;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.salamander.cem.client.CemResources;

@Mixin(SimpleReloadableResourceManager.class)
public abstract class MixinReloadableCemPacks {

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void salamander$packs(List<IResourcePack> packs, CallbackInfo ci) {
        CemResources.packs(packs);
    }
}
