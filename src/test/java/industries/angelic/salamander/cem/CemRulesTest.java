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
                new CemRules(properties).select(new Facts());
                count++;
            }
        }
        assertEquals(15, count);
    }

    private static CemRules rules(String text) throws Exception {
        Properties p = new Properties();
        p.load(new StringReader(text));
        return new CemRules(p);
    }

    private static final class Facts implements CemRules.Facts {

        final Map<String, String> values = new HashMap<>(), nbt = new HashMap<>();
        long seed;

        @Override
        public String value(String key) {
            return values.get(key);
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
