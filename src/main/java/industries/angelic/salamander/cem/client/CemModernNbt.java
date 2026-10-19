package industries.angelic.salamander.cem.client;

import java.util.List;
import java.util.TreeSet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import industries.angelic.salamander.compat.backhand.BackhandCompat;
import industries.angelic.salamander.mixins.early.minecraft.client.AccessorCemNbtList;

/**
 * Entity NBT as packs written for newer versions read it. Items carry string ids and a {@code count}; players get
 * {@code SelectedItem}, the off hand ({@code equipment.offhand}, and slot -106 in {@code Inventory}) and
 * {@code SleepingX/Y/Z}; mobs get {@code HandItems}, {@code ArmorItems} and {@code equipment}. Raw values are written
 * as newer versions write SNBT, with sorted keys, so regexes over them see the same order.
 */
final class CemModernNbt {

    private static final String[] ARMOR = { "feet", "legs", "chest", "head" };

    private CemModernNbt() {}

    static void add(Entity entity, NBTTagCompound nbt) {
        if (!(entity instanceof EntityLivingBase)) return;
        EntityLivingBase living = (EntityLivingBase) entity;
        NBTTagCompound equipment = new NBTTagCompound();
        for (int slot = 1; slot <= 4; slot++) put(equipment, ARMOR[slot - 1], living.getEquipmentInSlot(slot));
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            ItemStack offhand = BackhandCompat.offhandItem(player);
            put(equipment, "offhand", offhand);
            if (player.getHeldItem() != null) nbt.setTag("SelectedItem", item(player.getHeldItem(), null));
            if (nbt.hasKey("Inventory", 9)) {
                NBTTagList inventory = nbt.getTagList("Inventory", 10);
                for (int i = 0; i < inventory.tagCount(); i++) modernize(inventory.getCompoundTagAt(i));
                if (offhand != null) inventory.appendTag(item(offhand, (byte) -106));
            }
            if (player.isPlayerSleeping() && player.playerLocation != null) {
                nbt.setInteger("SleepingX", player.playerLocation.posX);
                nbt.setInteger("SleepingY", player.playerLocation.posY);
                nbt.setInteger("SleepingZ", player.playerLocation.posZ);
            }
        } else {
            put(equipment, "mainhand", living.getHeldItem());
            NBTTagList hands = new NBTTagList();
            hands.appendTag(item(living.getHeldItem(), null));
            hands.appendTag(new NBTTagCompound());
            nbt.setTag("HandItems", hands);
            NBTTagList armor = new NBTTagList();
            for (int slot = 1; slot <= 4; slot++) armor.appendTag(item(living.getEquipmentInSlot(slot), null));
            nbt.setTag("ArmorItems", armor);
        }
        nbt.setTag("equipment", equipment);
    }

    /** Newer item NBT: the registry name as {@code id}, and {@code count}. */
    private static NBTTagCompound item(ItemStack stack, Byte slot) {
        NBTTagCompound tag = new NBTTagCompound();
        if (stack == null || stack.getItem() == null) return tag;
        if (slot != null) tag.setByte("Slot", slot);
        tag.setString("id", name(stack.getItem()));
        tag.setInteger("count", stack.stackSize);
        return tag;
    }

    private static void put(NBTTagCompound equipment, String key, ItemStack stack) {
        if (stack != null && stack.getItem() != null) equipment.setTag(key, item(stack, null));
    }

    private static void modernize(NBTTagCompound entry) {
        Item item = Item.getItemById(entry.getShort("id"));
        if (item == null) return;
        entry.setString("id", name(item));
        entry.setInteger("count", entry.getByte("Count"));
    }

    private static String name(Item item) {
        Object name = Item.itemRegistry.getNameForObject(item);
        return name == null ? "minecraft:air" : name.toString();
    }

    /** SNBT as newer versions write it: sorted compound keys, plain lists, typed numbers. */
    static String snbt(NBTBase tag) {
        StringBuilder out = new StringBuilder();
        snbt(tag, out);
        return out.toString();
    }

    private static void snbt(NBTBase tag, StringBuilder out) {
        switch (tag.getId()) {
            case 1:
                out.append(((NBTBase.NBTPrimitive) tag).func_150290_f())
                    .append('b');
                break;
            case 2:
                out.append(((NBTBase.NBTPrimitive) tag).func_150289_e())
                    .append('s');
                break;
            case 3:
                out.append(((NBTBase.NBTPrimitive) tag).func_150287_d());
                break;
            case 4:
                out.append(((NBTBase.NBTPrimitive) tag).func_150291_c())
                    .append('L');
                break;
            case 5:
                out.append(((NBTBase.NBTPrimitive) tag).func_150288_h())
                    .append('f');
                break;
            case 6:
                out.append(((NBTBase.NBTPrimitive) tag).func_150286_g())
                    .append('d');
                break;
            case 7: {
                byte[] bytes = ((NBTTagByteArray) tag).func_150292_c();
                out.append("[B;");
                for (int i = 0; i < bytes.length; i++) out.append(i == 0 ? "" : ",")
                    .append(bytes[i])
                    .append('b');
                out.append(']');
                break;
            }
            case 8:
                quote(((NBTTagString) tag).func_150285_a_(), out);
                break;
            case 9: {
                List<NBTBase> elements = ((AccessorCemNbtList) tag).salamander$elements();
                out.append('[');
                for (int i = 0; i < elements.size(); i++) {
                    if (i > 0) out.append(',');
                    snbt(elements.get(i), out);
                }
                out.append(']');
                break;
            }
            case 10: {
                NBTTagCompound compound = (NBTTagCompound) tag;
                out.append('{');
                boolean first = true;
                for (Object key : new TreeSet<Object>(compound.func_150296_c())) {
                    if (!first) out.append(',');
                    first = false;
                    String name = key.toString();
                    if (name.matches("[A-Za-z0-9._+-]+")) out.append(name);
                    else quote(name, out);
                    out.append(':');
                    snbt(compound.getTag(name), out);
                }
                out.append('}');
                break;
            }
            case 11: {
                int[] ints = ((NBTTagIntArray) tag).func_150302_c();
                out.append("[I;");
                for (int i = 0; i < ints.length; i++) out.append(i == 0 ? "" : ",")
                    .append(ints[i]);
                out.append(']');
                break;
            }
            default:
                out.append(tag);
        }
    }

    private static void quote(String value, StringBuilder out) {
        out.append('"');
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\');
            out.append(c);
        }
        out.append('"');
    }
}
