package industries.angelic.salamander.cem.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

/** Renamed vanilla textures used by modern CEM packs, restricted to existing 1.7 targets. */
final class CemTextureAliases {

    private static final Map<String, List<String>> NAMES = new HashMap<>();
    static {
        alias("steve", "player/wide/steve");
        alias("cow/cow", "cow/cow_temperate");
        alias("cow/mooshroom", "cow/mooshroom_red");
        alias("cow/brown_mooshroom", "cow/mooshroom_brown");
        alias("pig/pig", "pig/pig_temperate");
        alias("pig/pig_saddle", "equipment/pig_saddle/saddle");
        alias("chicken", "chicken/chicken_temperate", "chicken/chicken");
        alias("bat", "bat/bat");
        alias("blaze", "blaze/blaze");
        alias("iron_golem", "iron_golem/iron_golem");
        alias("snowman", "snow_golem/snow_golem");
        alias("witch", "witch/witch");
        alias("squid", "squid/squid");
        alias("pig/pigzombie", "piglin/zombified_piglin");
        alias("zombie_pigman", "piglin/zombified_piglin");
        alias("cat/black", "cat/cat_black");
        alias("cat/red", "cat/cat_red");
        alias("cat/siamese", "cat/cat_siamese");
        alias("zombie/zombie_villager", "zombie_villager/zombie_villager");
        for (String name : new String[] { "villager", "farmer", "librarian", "priest", "smith", "butcher" })
            alias("villager/" + name, "villager/villager");
        alias("horse/armor/horse_armor_iron", "equipment/horse_body/iron");
        alias("horse/armor/horse_armor_gold", "equipment/horse_body/gold");
        alias("horse/armor/horse_armor_diamond", "equipment/horse_body/diamond");
    }

    private CemTextureAliases() {}

    private static void alias(String old, String... names) {
        List<String> paths = new ArrayList<>();
        for (String name : names) paths.add("textures/entity/" + name + ".png");
        NAMES.put("textures/entity/" + old + ".png", paths);
    }

    static List<String> candidates(ResourceLocation nativeTexture) {
        return nativeTexture == null ? Collections.emptyList()
            : NAMES.getOrDefault(nativeTexture.getResourcePath(), Collections.emptyList());
    }
}
