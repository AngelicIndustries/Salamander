package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Assume;
import org.junit.Test;

import industries.angelic.salamander.cem.loading.CemRules;

public class CemRulesTest {

    @Test
    public void orderedRulesPreserveIndexWhenChoosingBaseModel() throws Exception {
        CemRules rules = rules(
            "models.2=1\nblocks.2=grass_block farmland\nnbt.2.HasNectar=0\nmodels.7=2-3\nweights.7=1 3\nname.7=ipattern:worker*\n");
        Facts facts = new Facts();
        facts.values.put("blocks", "minecraft:grass:0");
        facts.nbt.put("HasNectar", "0");
        facts.values.put("name", "Worker Bee");
        assertEquals(2, rules.select(facts).rule);
        assertEquals(1, rules.select(facts).model);
        facts.nbt.put("HasNectar", "1");
        facts.seed = 3;
        assertEquals(7, rules.select(facts).rule);
        assertEquals(3, rules.select(facts).model);
        facts.values.remove("name");
        assertEquals(0, rules.select(facts).rule);
    }

    @Test
    public void rangesNegativeMatchesMissingNbtAndRawTags() throws Exception {
        CemRules rules = rules(
            "models.1=2\nheights.1=(-64)-(-1) 10-20\nhealth.1=0-50%\nnbt.1.Pumpkin=raw:1b\nnbt.1.sleeping_pos=exists:false\nname.1=!regex:Bad.*\n");
        Facts facts = new Facts();
        facts.values.put("heights", "-12");
        facts.values.put("healthPercent", "25");
        facts.values.put("name", "Good");
        facts.nbt.put("Pumpkin", "1b");
        assertEquals(2, rules.select(facts).model);
        facts.nbt.put("sleeping_pos", "position");
        assertEquals(1, rules.select(facts).model);
        assertTrue(
            CemRules.match("pattern:a?c*")
                .test("abcd"));
        assertFalse(
            CemRules.match("pattern:a?c*")
                .test("ac"));
    }

    @Test
    public void parsesAllFreshAnimationsRules() throws Exception {
        String directory = System.getenv("SALAMANDER_CEM_PACK_DIR");
        Assume.assumeNotNull(directory);
        int count = 0;
        try (ZipFile zip = new ZipFile(new File(directory, "FreshAnimations_v1.10.5.zip"))) {
            for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName()
                    .contains("/optifine/cem/")
                    || !entry.getName()
                        .endsWith(".properties"))
                    continue;
                Properties properties = new Properties();
                try (InputStream stream = zip.getInputStream(entry)) {
                    properties.load(stream);
                }
                CemRules rules = new CemRules(properties);
                rules.select(new Facts());
                assertTrue(entry.getName() + ": " + rules.warnings, rules.warnings.isEmpty());
                count++;
            }
        }
        assertEquals(15, count);
    }

    @Test
    public void skipsWhatItCannotReadAsEtfDoes() throws Exception {
        CemRules rules = rules(
            "models.1=2\nbiomes.1=plains\nscreamingGoat.1=true\nbaby.1=maybe\nheights.1=high\n"
                + "models.2=garbage\nname.2=Bob\nmodels.3=3\nweights.3=1 2 3\n");
        assertEquals(rules.warnings.toString(), 5, rules.warnings.size());
        Facts facts = new Facts();
        facts.values.put("biomes", "plains");
        // The unsupported and unreadable conditions are left out, so only the biome decides.
        assertEquals(2, rules.select(facts).model);
        facts.values.put("biomes", "desert");
        // Rule 2 could not be read and is gone; rule 3 keeps its first weight.
        assertEquals(3, rules.select(facts).rule);
        assertEquals(3, rules.select(facts).model);
    }

    @Test
    public void weightsArePaddedTrimmedOrDroppedAsEtfDoes() throws Exception {
        Facts facts = new Facts();
        CemRules padded = rules("models.1=2 3 4\nweights.1=2\n");
        facts.seed = 2;
        assertEquals(3, padded.select(facts).model);
        CemRules zero = rules("models.1=2 3\nweights.1=0 0\n");
        facts.seed = 1;
        assertEquals(3, zero.select(facts).model);
        CemRules negative = rules("models.1=2 3\nweights.1=-1 5\n");
        assertEquals(3, negative.select(facts).model);
        assertEquals(1, negative.warnings.size());
    }

    @Test
    public void acceptsEtfPropertyAliases() throws Exception {
        CemRules rules = rules(
            "models.1=2\nbiome.1=plains\nnames.1=Bob\nis_angry.1=true\ndayMonth.1=1-31\nmax_health.1=10-20\n");
        assertTrue(rules.warnings.toString(), rules.warnings.isEmpty());
        Facts facts = new Facts();
        facts.values.put("biomes", "plains");
        facts.values.put("name", "Bob");
        facts.values.put("angry", "true");
        facts.values.put("monthDay", "5");
        facts.values.put("maxHealth", "20");
        assertEquals(2, rules.select(facts).model);
        facts.values.put("angry", "false");
        assertEquals(1, rules.select(facts).model);
    }

    @Test
    public void seedOffsetAndVehicleSeedPickAsEtfDoes() throws Exception {
        String choices = "models.1=2 3 4 5 6 7 8 9\n";
        Facts facts = new Facts();
        facts.seed = 5;
        facts.vehicleSeed = 6;
        assertEquals(7, rules(choices).select(facts).model);
        assertEquals(8, rules(choices + "seedSource.1=vehicle\n").select(facts).model);
        int expected = 2 + Math.abs(5 ^ optifineHash(1)) % 8;
        assertEquals(expected, rules(choices + "seedOffset.1=1\n").select(facts).model);
    }

    /** ETF's copy of OptiFine's integer hash, kept separate from the code under test. */
    private static int optifineHash(int x) {
        x ^= 0x3D ^ x >> 16;
        x += x << 3;
        x ^= x >> 4;
        x *= 668265261;
        x ^= x >> 15;
        return x;
    }

    @Test
    public void etfKeysForModsPacksAndGameState() throws Exception {
        CemRules rules = rules(
            "models.1=2\nmodLoaded.1=angelica\nresourcepack.1=file/FA.zip\ndifficulty.1=2-3\nclientGameMode.1=1\n"
                + "usingShaders.1=true\nhardcore.1=false\nteammate.1=false\nblockBelow.1=grass_block\n");
        assertTrue(rules.warnings.toString(), rules.warnings.isEmpty());
        Facts facts = new Facts();
        facts.lists.put("modLoaded", java.util.Arrays.asList("salamander", "angelica"));
        facts.lists.put("resourcepack", java.util.Arrays.asList("vanilla", "FA.zip", "file/FA.zip"));
        facts.values.put("difficulty", "3");
        facts.values.put("clientGameMode", "1");
        facts.values.put("usingShaders", "true");
        facts.values.put("hardcore", "false");
        facts.values.put("teammate", "false");
        facts.lists.put("blockBelow", Collections.singletonList("minecraft:grass:0"));
        assertEquals(2, rules.select(facts).model);
        facts.lists.put("modLoaded", Collections.singletonList("salamander"));
        assertEquals(1, rules.select(facts).model);
    }

    private static CemRules rules(String text) throws Exception {
        Properties p = new Properties();
        p.load(new StringReader(text));
        return new CemRules(p);
    }

    private static final class Facts implements CemRules.Facts {

        final Map<String, String> values = new HashMap<>(), nbt = new HashMap<>();
        final Map<String, List<String>> lists = new HashMap<>();
        long seed, vehicleSeed;

        @Override
        public String value(String key) {
            return values.get(key);
        }

        @Override
        public List<String> values(String key) {
            if (lists.containsKey(key)) return lists.get(key);
            return CemRules.Facts.super.values(key);
        }

        @Override
        public long vehicleSeed() {
            return vehicleSeed;
        }

        @Override
        public List<String> nbt(String key, boolean raw) {
            String value = nbt.get(key);
            return value == null ? Collections.emptyList() : Collections.singletonList(value);
        }

        @Override
        public long seed() {
            return seed;
        }
    }
}
