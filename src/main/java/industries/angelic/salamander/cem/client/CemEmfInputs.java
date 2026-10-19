package industries.angelic.salamander.cem.client;

import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import industries.angelic.salamander.compat.backhand.BackhandCompat;
import industries.angelic.salamander.mixins.early.minecraft.client.AccessorCemEntityLivingBase;

/**
 * Entity variables EMF adds over OptiFine, with EMF's meaning. Everything in 1.7.10 is right-handed, and only
 * Backhand gives players an off hand.
 */
final class CemEmfInputs {

    private static final int WORLD_TOP = 256;

    private CemEmfInputs() {}

    static void put(EntityLivingBase living, float partialTicks, double swingProgress, Map<String, Double> values) {
        EntityPlayer player = living instanceof EntityPlayer ? (EntityPlayer) living : null;
        boolean offhandInUse = BackhandCompat.offhandInUse(player);
        float offhandSwing = BackhandCompat.offhandSwing(player, partialTicks);
        boolean using = player != null && (player.isUsingItem() || offhandInUse);
        // The hand used last: the off hand while it is in use, or swinging on its own.
        boolean mainHand = !(offhandInUse || offhandSwing != 0 && swingProgress == 0);
        boolean active = swingProgress != 0 || offhandSwing != 0 || using;
        ItemStack offhand = BackhandCompat.offhandItem(player);
        values.put("is_right_handed", 1d);
        values.put("is_using_item", using ? 1d : 0d);
        values.put("is_blocking", player != null && player.isBlocking() ? 1d : 0d);
        values.put("is_swinging_right_arm", active && mainHand ? 1d : 0d);
        values.put("is_swinging_left_arm", active && !mainHand ? 1d : 0d);
        values.put("is_holding_item_right", living.getHeldItem() != null ? 1d : 0d);
        values.put("is_holding_item_left", offhand != null ? 1d : 0d);
        values.put("is_jumping", ((AccessorCemEntityLivingBase) living).salamander$isJumping() ? 1d : 0d);
        fluidDepth(living, values);
    }

    /** Fluid blocks from the entity's feet down and up, counting the one it stands in; 0 out of fluid. */
    private static void fluidDepth(Entity entity, Map<String, Double> values) {
        World world = entity.worldObj;
        int x = MathHelper.floor_double(entity.posX);
        int y = MathHelper.floor_double(entity.boundingBox.minY);
        int z = MathHelper.floor_double(entity.posZ);
        if (world == null || !liquid(world, x, y, z)) {
            values.put("fluid_depth", 0d);
            values.put("fluid_depth_up", 0d);
            values.put("fluid_depth_down", 0d);
            return;
        }
        int up = y;
        while (liquid(world, x, up, z) && up < WORLD_TOP) up++;
        int down = y;
        while (liquid(world, x, down, z) && down > 0) down--;
        values.put("fluid_depth_up", (double) (up - y));
        values.put("fluid_depth_down", (double) (y - down));
        values.put("fluid_depth", (double) (y - down + up - y - 1));
    }

    private static boolean liquid(World world, int x, int y, int z) {
        return world.getBlock(x, y, z)
            .getMaterial()
            .isLiquid();
    }
}
