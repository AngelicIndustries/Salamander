package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import industries.angelic.salamander.cem.loading.CemRules;
import industries.angelic.salamander.cem.loading.CemTextureSet;

public class CemTextureSetTest {

    private static final ResourceLocation COW = id("textures/entity/cow/cow.png");

    @Test
    public void numberedTexturesUseStableSeedAndStopAtFirstGap() throws Exception {
        Resources resources = new Resources().add(COW.toString(), 0)
            .add("optifine/random/entity/cow/cow2.png", 1)
            .add("optifine/random/entity/cow/cow3.png", 1)
            .add("optifine/random/entity/cow/cow5.png", 1);
        CemTextureSet set = CemTextureSet.load(Collections.singletonList(COW), resources);
        for (int seed = 0; seed < 15; seed++) {
            CemRules.Selection selected = set.select(new Facts(seed), null);
            assertEquals(seed % 3 + 1, selected.model);
            assertEquals(
                selected.model == 1 ? COW : id("optifine/random/entity/cow/cow" + selected.model + ".png"),
                set.texture(selected));
        }
    }

    @Test
    public void propertiesAreOrderedWeightedAndAllowSparseSuffixes() throws Exception {
        Resources resources = new Resources().add(COW.toString(), 0)
            .rules(
                "etf/random/entity/cow/cow.properties",
                1,
                "skins.2=5 9\nweights.2=1 3\nname.2=ipattern:gold*\ntextures.7=2\nhealth.7=0-50%\n")
            .add("etf/random/entity/cow/cow5.png", 1)
            .add("etf/random/entity/cow/cow9.png", 1)
            .add("etf/random/entity/cow/cow2.png", 1);
        CemTextureSet set = CemTextureSet.load(Collections.singletonList(COW), resources);
        Facts facts = new Facts(3);
        assertEquals(COW, set.texture(set.select(facts, null)));
        facts.values.put("healthPercent", "20");
        assertEquals(2, set.select(facts, null).model);
        facts.values.put("name", "Golden Cow");
        assertEquals(9, set.select(facts, null).model);
        assertEquals(2, set.select(facts, null).rule);
    }

    @Test
    public void packPriorityBeatsDirectoryAndNativeTextureCanSuppressLowerPack() throws Exception {
        Resources resources = new Resources().add(COW.toString(), 0)
            .add("etf/random/entity/cow/cow2.png", 1)
            .add("optifine/random/entity/cow/cow2.png", 2);
        assertEquals(
            id("optifine/random/entity/cow/cow2.png"),
            CemTextureSet.load(Collections.singletonList(COW), resources)
                .texture(new CemRules.Selection(2, 0)));
        resources.add(COW.toString(), 3)
            .add("textures/entity/cow/cow2.png", 1);
        assertEquals(
            COW,
            CemTextureSet.load(Collections.singletonList(COW), resources)
                .texture(new CemRules.Selection(2, 0)));
    }

    @Test
    public void directoryPreferenceAndModNamespaceArePreserved() throws Exception {
        ResourceLocation bee = new ResourceLocation("etfuturum", "textures/entity/bee/bee.png");
        Resources resources = new Resources().add(bee.toString(), 0)
            .add("etfuturum:etf/random/entity/bee/bee2.png", 1)
            .add("etfuturum:optifine/random/entity/bee/bee2.png", 1);
        assertEquals(
            new ResourceLocation("etfuturum", "etf/random/entity/bee/bee2.png"),
            CemTextureSet.load(Collections.singletonList(bee), resources)
                .texture(new CemRules.Selection(2, 0)));
    }

    @Test
    public void higherPriorityModernBaseSuppressesLowerPriorityLegacyVariants() throws Exception {
        ResourceLocation modern = id("textures/entity/cow/cow_temperate.png");
        Resources resources = new Resources().add(COW.toString(), 0)
            .add(modern.toString(), 3)
            .add("optifine/random/entity/cow/cow2.png", 1);
        CemTextureSet set = CemTextureSet.load(Arrays.asList(COW, modern), resources);
        assertEquals(1, set.select(new Facts(1), null).model);
        assertEquals(COW, set.texture(new CemRules.Selection(2, 0)));
    }

    @Test
    public void modernAliasesAndLegacyFoldersWork() throws Exception {
        Resources resources = new Resources().add(COW.toString(), 0)
            .add("mcpatcher/mob/cow/cow_temperate2.png", 1);
        CemTextureSet set = CemTextureSet
            .load(Arrays.asList(COW, id("textures/entity/cow/cow_temperate.png")), resources);
        assertEquals(id("mcpatcher/mob/cow/cow_temperate2.png"), set.texture(new CemRules.Selection(2, 0)));
        assertEquals(COW, set.texture(new CemRules.Selection(1, 0)));
    }

    @Test
    public void stateTexturesInheritRulesAndLayersFollowSparseBodyChoice() throws Exception {
        ResourceLocation angry = id("textures/entity/wolf/wolf_angry.png");
        Resources resources = new Resources().add(angry.toString(), 0)
            .rules("optifine/random/entity/wolf/wolf.properties", 1, "textures.1=5\n")
            .add("optifine/random/entity/wolf/wolf_angry5.png", 1);
        CemTextureSet set = CemTextureSet.load(Collections.singletonList(angry), resources);
        assertEquals(id("optifine/random/entity/wolf/wolf_angry5.png"), set.texture(set.select(new Facts(0), null)));
        ResourceLocation wool = id("textures/entity/sheep/sheep_fur.png");
        resources.add(wool.toString(), 0)
            .add("optifine/random/entity/sheep/sheep_fur5.png", 1);
        resources.rules("etf/random/entity/sheep/sheep.properties", 1, "textures.1=5\n");
        set = CemTextureSet.load(Collections.singletonList(wool), resources);
        assertEquals(
            id("optifine/random/entity/sheep/sheep_fur5.png"),
            set.texture(set.select(new Facts(0), new CemRules.Selection(5, 2))));
        assertEquals(wool, set.texture(new CemRules.Selection(8, 2)));
    }

    @Test
    public void explicitLayerRulesOverrideBodyAndMissingVariantsFallBack() throws Exception {
        ResourceLocation wool = id("textures/entity/sheep/sheep_fur.png");
        Resources resources = new Resources().add(wool.toString(), 0)
            .rules("optifine/random/entity/sheep/sheep_fur.properties", 1, "textures.1=3\n")
            .add("optifine/random/entity/sheep/sheep_fur3.png", 1);
        CemTextureSet set = CemTextureSet.load(Collections.singletonList(wool), resources);
        assertEquals(3, set.select(new Facts(0), new CemRules.Selection(2, 1)).model);
        assertEquals(wool, set.texture(new CemRules.Selection(42, 1)));
    }

    @Test
    public void explicitStateRulesBeatInheritedRulesAcrossDirectories() throws Exception {
        ResourceLocation angry = id("textures/entity/wolf/wolf_angry.png");
        Resources resources = new Resources().add(angry.toString(), 0)
            .rules("etf/random/entity/wolf/wolf.properties", 1, "skins.1=5\n")
            .add("optifine/random/entity/wolf/wolf_angry2.png", 1)
            .add("optifine/random/entity/wolf/wolf_angry5.png", 1);
        CemTextureSet inherited = CemTextureSet.load(Collections.singletonList(angry), resources);
        assertEquals(5, inherited.select(new Facts(0), null).model);
        resources.rules("optifine/random/entity/wolf/wolf_angry.properties", 1, "textures.1=2\n");
        CemTextureSet explicit = CemTextureSet.load(Collections.singletonList(angry), resources);
        assertEquals(2, explicit.select(new Facts(0), null).model);
    }

    @Test
    public void numericBaseNamesUseSeparator() {
        assertEquals(
            id("textures/entity/example_2.3.png"),
            CemTextureSet.variant(id("textures/entity/example_2.png"), 3));
    }

    @Test
    public void malformedRulesFailInsteadOfRandomizingUnconditionally() throws Exception {
        Resources resources = new Resources().add(COW.toString(), 0)
            .rules("optifine/random/entity/cow/cow.properties", 1, "textures.1=2\nunsupportedThing.1=true\n");
        assertThrows(
            IllegalArgumentException.class,
            () -> CemTextureSet.load(Collections.singletonList(COW), resources));
    }

    @Test
    public void overlayTextureOverridesKeepInheritedRules() throws Exception {
        Resources resources = new Resources().add(COW.toString(), 0)
            .rules("optifine/random/entity/cow/cow.properties", 1, "skins.1=2 5\nweights.1=1\n")
            .add("optifine/random/entity/cow/cow2.png", 2)
            .add("optifine/random/entity/cow/cow5.png", 1);
        CemTextureSet set = CemTextureSet.load(Collections.singletonList(COW), resources);
        assertEquals(5, set.select(new Facts(1), null).model);
        assertEquals(id("optifine/random/entity/cow/cow5.png"), set.texture(set.select(new Facts(1), null)));
    }

    @Test
    public void etfPredicatesAndModernBiomeAliasesMatchLegacyFacts() throws Exception {
        Resources resources = new Resources().add(COW.toString(), 0)
            .rules(
                "etf/random/entity/cow/cow.properties",
                1,
                "textures.1=2\nbiomes.1=snowy_plains\ndimension.1=minecraft:overworld\n"
                    + "teams.1=\"blue team\" red\nspeed.1=0.1 - 0.5\nmaxHealth.1=10-40\n"
                    + "distance.1=0-20\ncreeperCharged.1=false\nisAngry.1=false\nmoving.1=true\n");
        CemTextureSet set = CemTextureSet.load(Collections.singletonList(COW), resources);
        Facts facts = new Facts(0);
        facts.values.put("biomes", "Ice Plains");
        facts.values.put("dimension", "overworld");
        facts.values.put("teams", "blue team");
        facts.values.put("speed", "0.25");
        facts.values.put("maxHealth", "20");
        facts.values.put("distanceFromPlayer", "3");
        facts.values.put("creeperCharged", "false");
        facts.values.put("angry", "false");
        facts.values.put("moving", "true");
        assertEquals(2, set.select(facts, null).model);
        facts.values.put("distanceFromPlayer", "50");
        assertEquals(1, set.select(facts, null).model);
        assertTrue(
            CemRules.range("-65--2 1-4")
                .test("-10"));
    }

    @Test
    public void parsesOriginalCozyCrittersTextureRules() throws Exception {
        String directory = System.getenv("SALAMANDER_CEM_PACK_DIR");
        org.junit.Assume.assumeNotNull(directory);
        java.io.File file = new java.io.File(directory, "cozy-critters-1-19-4.zip");
        org.junit.Assume.assumeTrue(file.isFile());
        int count = 0;
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(file)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                if (!entry.getName()
                    .contains("/random/")
                    || !entry.getName()
                        .endsWith(".properties"))
                    continue;
                Properties properties = new Properties();
                try (java.io.InputStream stream = zip.getInputStream(entry)) {
                    properties.load(stream);
                }
                CemRules rules = new CemRules(properties, true);
                for (int seed = 0; seed < 100; seed++) {
                    Facts facts = new Facts(seed);
                    facts.values.put("baby", String.valueOf(seed % 2 == 0));
                    facts.values.put("biomes", seed % 3 == 0 ? "Ice Plains" : "Plains");
                    int variant = rules.select(facts).model;
                    if (variant > 1) assertNotNull(
                        entry.getName() + " suffix " + variant,
                        zip.getEntry(
                            entry.getName()
                                .replace(".properties", variant + ".png")));
                }
                count++;
            }
        }
        assertEquals(9, count);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(path);
    }

    private static final class Resources implements CemTextureSet.Resources {

        final Map<ResourceLocation, Integer> ranks = new HashMap<>();
        final Map<ResourceLocation, Properties> rules = new HashMap<>();

        Resources add(String name, int rank) {
            ranks.put(id(name), rank);
            return this;
        }

        Resources rules(String name, int rank, String text) throws IOException {
            add(name, rank);
            Properties p = new Properties();
            p.load(new StringReader(text));
            rules.put(id(name), p);
            return this;
        }

        @Override
        public boolean exists(ResourceLocation location) {
            return ranks.containsKey(location);
        }

        @Override
        public int priority(ResourceLocation location) {
            return ranks.getOrDefault(location, -1);
        }

        @Override
        public Properties properties(ResourceLocation location) {
            return rules.get(location);
        }
    }

    private static final class Facts implements CemRules.Facts {

        final long seed;
        final Map<String, String> values = new HashMap<>();

        Facts(long seed) {
            this.seed = seed;
        }

        @Override
        public long seed() {
            return seed;
        }

        @Override
        public String value(String key) {
            return values.get(key);
        }

        @Override
        public List<String> nbt(String path, boolean raw) {
            return Collections.emptyList();
        }
    }
}
