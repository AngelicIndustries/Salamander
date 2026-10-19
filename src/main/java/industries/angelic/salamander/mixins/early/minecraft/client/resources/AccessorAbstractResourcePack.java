package industries.angelic.salamander.mixins.early.minecraft.client.resources;

import java.io.File;

import net.minecraft.client.resources.AbstractResourcePack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes a resource pack's root without reflection or an access transformer. */
@Mixin(AbstractResourcePack.class)
public interface AccessorAbstractResourcePack {

    @Accessor("resourcePackFile")
    File salamander$getResourcePackFile();
}
