package industries.angelic.salamander.compat.aether;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.Salamander;

/**
 * The Aether's gloves and pendant, which it draws over the arms and the body on the vanilla model, read from its API.
 * Nothing is worn when the Aether is not loaded or its API has changed.
 */
@SideOnly(Side.CLIENT)
public final class AetherAccessories {

    private static final String API = "com.gildedgames.the_aether.api.";
    private static final boolean LOADED = Loader.isModLoaded("aether_legacy");
    private static boolean resolved;
    private static boolean failed;
    private static Method player;
    private static Method inventory;
    private static Method firstWorn;
    private static Object gloves;
    private static Object pendant;
    private static Class<?> accessory;
    private static Field texture;

    private AetherAccessories() {}

    public static ItemStack gloves(EntityPlayer wearer) {
        return worn(wearer, true);
    }

    public static ItemStack pendant(EntityPlayer wearer) {
        return worn(wearer, false);
    }

    /** The texture the Aether draws a worn accessory with. */
    public static ResourceLocation texture(ItemStack stack) {
        if (stack == null || !resolve() || !accessory.isInstance(stack.getItem())) return null;
        try {
            return (ResourceLocation) texture.get(stack.getItem());
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return null;
        }
    }

    private static ItemStack worn(EntityPlayer wearer, boolean hands) {
        if (!resolve()) return null;
        try {
            Object aether = player.invoke(null, wearer);
            Object worn = aether == null ? null : inventory.invoke(aether);
            return worn == null ? null : (ItemStack) firstWorn.invoke(worn, hands ? gloves : pendant);
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return null;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static boolean resolve() {
        if (!LOADED || failed) return false;
        if (resolved) return true;
        resolved = true;
        try {
            Class type = Class.forName(API + "accessories.AccessoryType");
            player = Class.forName(API + "AetherAPI")
                .getMethod("get", EntityPlayer.class);
            inventory = Class.forName(API + "player.IPlayerAether")
                .getMethod("getAccessoryInventory");
            firstWorn = Class.forName(API + "player.util.IAccessoryInventory")
                .getMethod("getFirstStackIfWearing", type);
            gloves = Enum.valueOf(type, "GLOVES");
            pendant = Enum.valueOf(type, "PENDANT");
            accessory = Class.forName("com.gildedgames.the_aether.items.accessories.ItemAccessory");
            texture = accessory.getField("texture");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            fail(e);
        }
        return !failed;
    }

    private static void fail(Throwable e) {
        if (failed) return;
        failed = true;
        Salamander.LOG.warn("The Aether's gloves and pendant could not be read; layers under them are drawn whole", e);
    }
}
