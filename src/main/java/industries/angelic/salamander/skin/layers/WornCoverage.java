package industries.angelic.salamander.skin.layers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.api.skin.SkinPart;
import industries.angelic.salamander.compat.aether.AetherAccessories;
import industries.angelic.salamander.skin.layers.Coverage.Box;

/**
 * What a player's armor and Aether accessories cover of each overlay part, worked out again when what it wears
 * changes. Armor covers as vanilla draws it: the helmet over the hat, the chestplate over the jacket and sleeves, the
 * leggings over the jacket and pants, boots over the pants. The Aether's gloves cover the sleeves, its pendant the
 * jacket. Armor on its own model, or anything but armor in an armor slot, covers its parts whole.
 */
@SideOnly(Side.CLIENT)
final class WornCoverage {

    private static final int GLOVES = 4;
    private static final int PENDANT = 5;
    /** Offered as the default armor model, to tell armor that brings its own. */
    private static final ModelBiped VANILLA_MODEL = new ModelBiped();
    private static final Map<EntityPlayer, WornCoverage> WORN = new WeakHashMap<>();

    private final ItemStack[] stacks;
    private final Item[] items;
    private final int[] damage;
    private final NBTTagCompound[] tags;
    private final Coverage[] world;
    private final Coverage[] firstPerson;

    private WornCoverage(EntityPlayer player, ItemStack[] stacks) {
        this.stacks = stacks;
        items = new Item[stacks.length];
        damage = new int[stacks.length];
        tags = new NBTTagCompound[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i] == null) continue;
            items[i] = stacks[i].getItem();
            damage[i] = stacks[i].getItemDamage();
            tags[i] = stacks[i].stackTagCompound;
        }

        Coverage.Builder[] world = builders();
        Coverage.Builder[] firstPerson = builders();
        for (int pass = 0; pass < 4; pass++) {
            ItemStack stack = stacks[pass];
            if (stack == null) continue;
            if (!(stack.getItem() instanceof ItemArmor armor)) {
                wear(world, pass, null);
                continue;
            }
            // Vanilla draws armor in this order, helmet first, and passes the order as the slot.
            List<ResourceLocation> textures = new ArrayList<>();
            textures.add(RenderBiped.getArmorResource(player, stack, pass, null));
            if (armor.getColor(stack) != -1) textures.add(RenderBiped.getArmorResource(player, stack, pass, "overlay"));
            ModelBiped model = ForgeHooksClient.getArmorModel(player, stack, pass, VANILLA_MODEL);
            if (model == VANILLA_MODEL) {
                for (ResourceLocation texture : textures) wear(world, pass, texture);
            } else {
                wear(world, pass, null);
            }
        }
        if (stacks[GLOVES] != null) {
            ResourceLocation gloves = AetherAccessories.texture(stacks[GLOVES]);
            for (Coverage.Builder[] parts : new Coverage.Builder[][] { world, firstPerson }) {
                parts[SkinPart.RIGHT_SLEEVE.ordinal()].add(gloves, Box.ARM);
                parts[SkinPart.LEFT_SLEEVE.ordinal()].add(gloves, Box.ARM);
            }
        }
        if (stacks[PENDANT] != null) {
            world[SkinPart.JACKET.ordinal()].add(AetherAccessories.texture(stacks[PENDANT]), Box.BODY);
        }
        this.world = build(world);
        this.firstPerson = build(firstPerson);
    }

    static WornCoverage of(EntityPlayer player) {
        ItemStack[] stacks = new ItemStack[6];
        if (player.inventory != null) {
            for (int pass = 0; pass < 4; pass++) stacks[pass] = player.inventory.armorItemInSlot(3 - pass);
        }
        stacks[GLOVES] = AetherAccessories.gloves(player);
        stacks[PENDANT] = AetherAccessories.pendant(player);
        WornCoverage worn = WORN.get(player);
        if (worn == null || !worn.wears(stacks)) {
            worn = new WornCoverage(player, stacks);
            WORN.put(player, worn);
        }
        return worn;
    }

    static void clear() {
        WORN.clear();
    }

    /**
     * In the world, everything worn covers the part. The first-person arm has no armor drawn on it, only the gloves
     * when {@code gloved}.
     */
    Coverage of(SkinPart part, boolean firstPerson, boolean gloved) {
        if (!firstPerson) return world[part.ordinal()];
        return gloved ? this.firstPerson[part.ordinal()] : Coverage.NONE;
    }

    private boolean wears(ItemStack[] now) {
        for (int i = 0; i < now.length; i++) {
            ItemStack stack = now[i];
            if (stack != stacks[i]) return false;
            if (stack != null && (stack.getItem() != items[i] || stack.getItemDamage() != damage[i]
                || stack.stackTagCompound != tags[i])) return false;
        }
        return true;
    }

    /** {@code texture} drawn by the armor in {@code pass}, or null for armor this cannot see into. */
    private static void wear(Coverage.Builder[] parts, int pass, ResourceLocation texture) {
        switch (pass) {
            case 0 -> parts[SkinPart.HAT.ordinal()].add(texture, Box.HEAD)
                .add(texture, Box.HEADWEAR);
            case 1 -> {
                parts[SkinPart.JACKET.ordinal()].add(texture, Box.BODY);
                parts[SkinPart.RIGHT_SLEEVE.ordinal()].add(texture, Box.ARM);
                parts[SkinPart.LEFT_SLEEVE.ordinal()].add(texture, Box.ARM);
            }
            case 2 -> {
                parts[SkinPart.JACKET.ordinal()].add(texture, Box.BODY);
                parts[SkinPart.RIGHT_PANTS.ordinal()].add(texture, Box.LEG);
                parts[SkinPart.LEFT_PANTS.ordinal()].add(texture, Box.LEG);
            }
            default -> {
                parts[SkinPart.RIGHT_PANTS.ordinal()].add(texture, Box.LEG);
                parts[SkinPart.LEFT_PANTS.ordinal()].add(texture, Box.LEG);
            }
        }
    }

    private static Coverage.Builder[] builders() {
        SkinPart[] parts = SkinPart.values();
        Coverage.Builder[] builders = new Coverage.Builder[parts.length];
        for (SkinPart part : parts) builders[part.ordinal()] = new Coverage.Builder(part);
        return builders;
    }

    private static Coverage[] build(Coverage.Builder[] builders) {
        Coverage[] coverage = new Coverage[builders.length];
        for (int i = 0; i < builders.length; i++) coverage[i] = builders[i].build();
        return coverage;
    }
}
