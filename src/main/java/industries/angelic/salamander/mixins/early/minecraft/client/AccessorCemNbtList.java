package industries.angelic.salamander.mixins.early.minecraft.client;

import java.util.List;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NBTTagList.class)
public interface AccessorCemNbtList {

    @Accessor("tagList")
    List<NBTBase> salamander$elements();
}
