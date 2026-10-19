package industries.angelic.salamander.mixins.early.minecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the client render timer without reflection or an access transformer. */
@Mixin(Minecraft.class)
public interface AccessorMinecraft {

    @Accessor("timer")
    Timer salamander$getTimer();
}
