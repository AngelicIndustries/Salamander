package industries.angelic.salamander.mixins.early.minecraft.client.resources;

import net.minecraft.client.resources.data.PackMetadataSectionSerializer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.google.gson.JsonElement;

import industries.angelic.salamander.cem.loading.CemPackMetadata;

@Mixin(PackMetadataSectionSerializer.class)
public abstract class MixinPackMetadataSectionSerializer {

    @ModifyVariable(
        method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/resources/data/PackMetadataSection;",
        at = @At("HEAD"),
        argsOnly = true)
    private JsonElement salamander$modernPackMetadata(JsonElement element) {
        return CemPackMetadata.legacyCompatible(element);
    }
}
