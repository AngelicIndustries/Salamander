package industries.angelic.salamander.compat.backhand;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.Loader;
import xonin.backhand.api.core.BackhandUtils;
import xonin.backhand.api.core.IBackhandPlayer;

/** Backhand's off hand. Without Backhand the off hand is empty and never in use. */
public final class BackhandCompat {

    private static final boolean LOADED = Loader.isModLoaded("backhand");

    private BackhandCompat() {}

    /** The item in the player's off hand, or null. */
    public static ItemStack offhandItem(EntityPlayer player) {
        return LOADED && player != null ? Api.offhandItem(player) : null;
    }

    public static boolean offhandInUse(EntityPlayer player) {
        return LOADED && player != null && Api.offhandInUse(player);
    }

    /** How far through its swing the off hand is, 0 when it isn't swinging. */
    public static float offhandSwing(EntityPlayer player, float partialTicks) {
        return LOADED && player != null ? Api.offhandSwing(player, partialTicks) : 0;
    }

    /** Only loaded with Backhand present. */
    private static final class Api {

        static ItemStack offhandItem(EntityPlayer player) {
            return BackhandUtils.getOffhandItem(player);
        }

        static boolean offhandInUse(EntityPlayer player) {
            return player instanceof IBackhandPlayer && ((IBackhandPlayer) player).isOffhandItemInUse();
        }

        static float offhandSwing(EntityPlayer player, float partialTicks) {
            return player instanceof IBackhandPlayer ? ((IBackhandPlayer) player).getOffSwingProgress(partialTicks) : 0;
        }
    }
}
