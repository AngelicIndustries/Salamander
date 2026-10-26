package industries.angelic.salamander.cem.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBook;
import net.minecraft.client.model.ModelHorse;
import net.minecraft.client.model.ModelSheep1;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.model.ModelSkeletonHead;
import net.minecraft.client.model.ModelSlime;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ResourceLocation;

/** Names are opt-in by exact entity class. Mods can register their own native model bindings. */
public final class CemTargets {

    private static final Map<String, Function<Object, String>> TARGETS = new LinkedHashMap<>();
    static {
        String[] mobs = { "Bat:bat", "Blaze:blaze", "CaveSpider:cave_spider", "Chicken:chicken", "Cow:cow",
            "Creeper:creeper", "Enderman:enderman", "Ghast:ghast", "IronGolem:iron_golem", "MagmaCube:magma_cube",
            "Mooshroom:mooshroom", "Pig:pig", "PigZombie:zombified_piglin", "Sheep:sheep", "Silverfish:silverfish",
            "Slime:slime", "Snowman:snow_golem", "Spider:spider", "Squid:squid", "Villager:villager", "Witch:witch",
            "Wither:wither", "Wolf:wolf" };
        for (String pair : mobs) {
            String[] p = pair.split(":");
            for (String pkg : new String[] { "monster", "passive", "boss" })
                register("net.minecraft.entity." + pkg + ".Entity" + p[0], p[1]);
        }
        register(
            "net.minecraft.entity.monster.EntityZombie",
            o -> ((EntityZombie) o).isVillager() ? "zombie_villager" : "zombie");
        register(
            "net.minecraft.entity.monster.EntitySkeleton",
            o -> ((EntitySkeleton) o).getSkeletonType() == 1 ? "wither_skeleton" : "skeleton");
        register("net.minecraft.entity.passive.EntityOcelot", o -> ((EntityOcelot) o).isTamed() ? "cat" : "ocelot");
        register(
            "net.minecraft.entity.passive.EntityHorse",
            o -> new String[] { "horse", "donkey", "mule", "zombie_horse", "skeleton_horse" }[Math
                .min(4, Math.max(0, ((EntityHorse) o).getHorseType()))]);
        register("net.minecraft.entity.monster.EntityGiantZombie", "giant");
        register("net.minecraft.entity.boss.EntityDragon", "dragon");
        register("net.minecraft.entity.item.EntityBoat", "boat");
        register("net.minecraft.entity.item.EntityEnderCrystal", "end_crystal");
        register("net.minecraft.entity.EntityLeashKnot", "lead_knot");
        register("net.minecraft.entity.projectile.EntityWitherSkull", "wither_skull");
        register("net.minecraft.entity.projectile.EntityArrow", "arrow");
        String[] carts = { "Empty:minecart", "Chest:chest_minecart", "Furnace:furnace_minecart", "TNT:tnt_minecart",
            "Hopper:hopper_minecart", "MobSpawner:spawner_minecart", "CommandBlock:command_block_minecart" };
        for (String pair : carts) {
            String[] p = pair.split(":");
            register("net.minecraft.entity.item.EntityMinecart" + p[0], p[1]);
        }
        register(
            "net.minecraft.tileentity.TileEntityChest",
            o -> ((TileEntityChest) o).func_145980_j() == 1 ? "trapped_chest" : "chest");
        register("net.minecraft.tileentity.TileEntityEnderChest", "ender_chest");
        register(
            "net.minecraft.tileentity.TileEntitySign",
            o -> ((TileEntitySign) o).getBlockType() == net.minecraft.init.Blocks.wall_sign ? "wall_sign" : "sign");
        register("net.minecraft.tileentity.TileEntityEnchantmentTable", "enchanting_book");
        register(
            "net.minecraft.tileentity.TileEntitySkull",
            o -> new String[] { "head_skeleton", "head_wither_skeleton", "head_zombie", "head_player",
                "head_creeper" }[Math.min(4, Math.max(0, ((TileEntitySkull) o).func_145904_a()))]);
        register("ganymedes01.etfuturum.tileentities.TileEntityWoodSign", "sign");
        register("ganymedes01.etfuturum.tileentities.TileEntityBanner", "banner");
        register("ganymedes01.etfuturum.tileentities.TileEntityShulkerBox", "shulker_box");
        String[] efr = { "ArmourStand:armor_stand", "Endermite:endermite", "Rabbit:rabbit", "Husk:husk", "Stray:stray",
            "ZombieVillager:zombie_villager", "PlacedEndCrystal:end_crystal", "BrownMooshroom:mooshroom",
            "NewBoat:boat", "NewBoatWithChest:chest_boat", "Shulker:shulker", "ShulkerBullet:shulker_bullet", "Bee:bee",
            "NewSnowGolem:snow_golem", "Fox:fox", "RespawnedDragon:dragon", "TippedArrow:arrow" };
        for (String pair : efr) {
            String[] p = pair.split(":");
            register("ganymedes01.etfuturum.entities.Entity" + p[0], p[1]);
        }
    }

    private static final Map<String, List<String>> SADDLES = new java.util.HashMap<>();
    private static final List<String> BANNER = Collections
        .unmodifiableList(Arrays.asList("banner_base", "banner_flag"));

    private CemTargets() {}

    private static List<String> saddles(String target) {
        return Collections.unmodifiableList(Arrays.asList(target + "_baby_saddle", target + "_saddle"));
    }

    public static void register(String className, String target) {
        register(className, o -> target);
    }

    public static void register(String className, Function<Object, String> target) {
        TARGETS.put(className, target);
    }

    public static String target(Object subject) {
        Function<Object, String> f = subject == null ? null
            : TARGETS.get(
                subject.getClass()
                    .getName());
        return f == null ? null : f.apply(subject);
    }

    public static List<String> candidates(Object subject, ModelBase model, ResourceLocation texture) {
        return candidates(subject, target(subject), model, texture);
    }

    static List<String> candidates(CemBinding binding, Object subject, ModelBase model, ResourceLocation texture) {
        String target = target(subject);
        if (CemPlayers.model(subject, model)) return candidates(subject, target, model, texture);
        Class<?> type = subject == null ? null : subject.getClass();
        int flags = (subject instanceof EntityLivingBase && ((EntityLivingBase) subject).isChild() ? 1 : 0)
            | (model instanceof ModelSign && ((ModelSign) model).signStick.showModel ? 2 : 0);
        if (binding.candidates != null && binding.candidateType == type
            && binding.candidateFlags == flags
            && java.util.Objects.equals(binding.candidateTarget, target)
            && java.util.Objects.equals(binding.candidateTexture, texture)) return binding.candidates;
        List<String> result = candidates(subject, target, model, texture);
        binding.candidates = result;
        binding.candidateType = type;
        binding.candidateFlags = flags;
        binding.candidateTarget = target;
        binding.candidateTexture = texture;
        return result;
    }

    private static List<String> candidates(Object subject, String target, ModelBase model, ResourceLocation texture) {
        String tex = texture == null ? "" : texture.getResourcePath();
        if (CemPlayers.model(subject, model)) {
            if (!industries.angelic.salamander.config.CemConfig.playerModels) return Collections.emptyList();
            if (tex.startsWith("textures/models/armor/")) return Collections.emptyList();
            return Collections.singletonList(
                CemPlayers.slim((net.minecraft.client.entity.AbstractClientPlayer) subject, model) ? "player_slim"
                    : "player");
        }
        if (model instanceof ModelSkeletonHead || model.getClass()
            .getName()
            .endsWith(".ModelHead")) {
            if (!(subject instanceof net.minecraft.entity.projectile.EntityWitherSkull))
                target = tex.contains("wither_skeleton") ? "head_wither_skeleton"
                    : tex.contains("skeleton") ? "head_skeleton"
                        : tex.contains("zombie") ? "head_zombie"
                            : tex.contains("creeper") ? "head_creeper" : "head_player";
        }
        if (model instanceof ModelSign) target = ((ModelSign) model).signStick.showModel ? "sign" : "wall_sign";
        if (model.getClass()
            .getName()
            .endsWith(".ModelRaft") && target != null) target = target.equals("chest_boat") ? "chest_raft" : "raft";
        if (target == null && model instanceof ModelBook) target = "enchanting_screen_book";
        if (target == null) return Collections.emptyList();
        String layer = "";
        if (tex.startsWith("textures/models/armor/")) return Collections.emptyList();
        if (target.equals("sheep") && model instanceof ModelSheep1) layer = "_wool";
        if (target.equals("pig") && tex.contains("saddle")) layer = "_saddle";
        if (target.equals("slime") && model instanceof ModelSlime
            && ((CemModelParts) model).salamander$cemParts()
                .get("right_eye") == null)
            layer = "_outer";
        if (target.equals("creeper") && tex.contains("armor")) layer = "_charge";
        if (target.equals("wither") && tex.contains("armor")) layer = "_armor";
        if (target.equals("wolf") && tex.contains("collar")) layer = "_collar";
        if (target.equals("stray") && tex.contains("overlay")) layer = "_outer";
        if (model instanceof ModelHorse && tex.contains("armor")) layer = "_armor";
        List<String> names = new ArrayList<>();
        if (subject instanceof EntityLivingBase && ((EntityLivingBase) subject).isChild())
            names.add(target + "_baby" + layer);
        if (target.equals("boat") || target.equals("chest_boat")) {
            String wood = tex.substring(tex.lastIndexOf('/') + 1)
                .replace(".png", "");
            if (Arrays
                .asList("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak")
                .contains(wood)) names.add(wood + "_" + target);
        }
        if (model instanceof ModelSign) {
            String wood = tex.substring(tex.lastIndexOf('/') + 1)
                .replace(".png", "")
                .replace("_sign", "");
            if (wood.equals("sign")) wood = "oak";
            names.add(wood + "_" + target);
        }
        names.add(target + layer);
        if (target.equals("lead_knot")) names.add("leash_knot");
        if (target.equals("wall_sign")) names.add("sign");
        if (layer.equals("_collar")) names.add(target);
        if (target.endsWith("_minecart")) names.add("minecart");
        if (target.equals("enchanting_screen_book")) names.add("enchanting_book");
        if (target.startsWith("enchanting_")) names.add("book");
        if (target.equals("zombified_piglin")) names.add("zombie_pigman");
        return names;
    }

    public static List<String> patchCandidates(Object subject, ModelBase model, ResourceLocation texture) {
        List<String> result = new ArrayList<>();
        for (String target : candidates(subject, model, texture))
            if (target.endsWith("boat")) result.add(target + "_patch");
        if (!result.contains("boat_patch")) result.add("boat_patch");
        return result;
    }

    public static List<String> extraCandidates(Object subject, ModelBase model) {
        if (model instanceof ModelHorse) {
            String target = target(subject);
            return target == null ? Collections.emptyList() : SADDLES.computeIfAbsent(target, CemTargets::saddles);
        }
        if (model.getClass()
            .getName()
            .endsWith(".ModelBanner")) return BANNER;
        return Collections.emptyList();
    }

    public static List<String> overlayCandidates(Object subject, ModelBase model, ResourceLocation texture) {
        if (model instanceof ModelHorse && subject instanceof EntityHorse
            && ((EntityHorse) subject).isHorseSaddled()
            && (texture == null || !texture.getResourcePath()
                .contains("armor"))) {
            String target = target(subject);
            if (target != null) return model.isChild ? Arrays.asList(target + "_baby_saddle", target + "_saddle")
                : Collections.singletonList(target + "_saddle");
        }
        return Collections.emptyList();
    }

    public static List<String> partCandidates(Object subject, ModelBase model, String name) {
        if (model.getClass()
            .getName()
            .endsWith(".ModelBanner"))
            return Collections.singletonList(name.equals("slate") ? "banner_flag" : "banner_base");
        return Collections.emptyList();
    }

}
