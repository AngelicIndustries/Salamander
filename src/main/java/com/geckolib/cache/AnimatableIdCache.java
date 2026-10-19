package com.geckolib.cache;

import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.DimensionManager;

import industries.angelic.salamander.Salamander;

/** Persistent overworld counter used to reserve unique singleton animation instance IDs. */
public final class AnimatableIdCache extends WorldSavedData {

    public static final String NAME = Salamander.MODID + "_animatable_ids";

    private static final String LAST_ID_KEY = "last_id";

    private long lastId;

    public AnimatableIdCache() {
        this(NAME);
    }

    public AnimatableIdCache(String name) {
        super(name);
    }

    public static long getFreeId(World world) {
        Objects.requireNonNull(world, "world");

        if (world.isRemote) throw new IllegalArgumentException("Animatable IDs can only be reserved on the server");

        AnimatableIdCache cache = getCache(canonicalStorageWorld(world));

        synchronized (cache) {
            cache.markDirty();

            return ++cache.lastId;
        }
    }

    private static AnimatableIdCache getCache(World world) {
        AnimatableIdCache cache = (AnimatableIdCache) world.loadItemData(AnimatableIdCache.class, NAME);

        if (cache == null) {
            cache = new AnimatableIdCache();
            world.setItemData(NAME, cache);
        }

        return cache;
    }

    private static World canonicalStorageWorld(World world) {
        World overworld = DimensionManager.getWorld(0);

        return overworld == null ? world : overworld;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.lastId = Math.max(0, compound.getLong(LAST_ID_KEY));
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        compound.setLong(LAST_ID_KEY, this.lastId);
    }
}
