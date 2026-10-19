package industries.angelic.salamander.cem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import industries.angelic.salamander.cem.animation.CemExpression;
import industries.angelic.salamander.cem.animation.CemExpressionParser;
import industries.angelic.salamander.cem.animation.CemNbtQueries;
import industries.angelic.salamander.cem.animation.CemNbtTest;

public class CemNbtMatchTest {

    /** One value per path, plain and raw. */
    private static CemNbtTest.Source source(String path, String plain, String raw) {
        return (asked, isRaw) -> asked.equals(path) ? Collections.singletonList(isRaw ? raw : plain)
            : Collections.<String>emptyList();
    }

    @Test
    public void rawRegexSeesSortedSnbt() {
        CemNbtTest.Source inventory = source(
            "Inventory",
            null,
            "[{Slot:0b,count:1,id:\"minecraft:bread\"},{Slot:-106b,count:1,id:\"etfuturum:shield\"}]");
        assertTrue(
            CemNbtTest.of("raw:iregex:.*Slot:-106b.*shield.*")
                .test(inventory, "Inventory"));
        assertFalse(
            CemNbtTest.of("raw:iregex:.*Slot:-106b.*spear.*")
                .test(inventory, "Inventory"));
    }

    @Test
    public void rawStringsAreUnquoted() {
        CemNbtTest.Source held = source("SelectedItem.id", "etfuturum:shield", "etfuturum:shield");
        assertTrue(
            CemNbtTest.of("raw:etfuturum:shield")
                .test(held, "SelectedItem.id"));
        assertTrue(
            CemNbtTest.of("raw:iregex:.*SHIELD.*")
                .test(held, "SelectedItem.id"));
    }

    @Test
    public void existsAndMissingValues() {
        CemNbtTest.Source sleeping = source("SleepingX", "12", "12");
        assertTrue(
            CemNbtTest.of("exists:true")
                .test(sleeping, "SleepingX"));
        assertFalse(
            CemNbtTest.of("exists:true")
                .test(sleeping, "SleepingY"));
        assertTrue(
            CemNbtTest.of("exists:false")
                .test(sleeping, "SleepingY"));
        // Any other test fails on a missing value, and its inversion passes.
        assertFalse(
            CemNbtTest.of("12")
                .test(sleeping, "SleepingY"));
        assertTrue(
            CemNbtTest.of("!12")
                .test(sleeping, "SleepingY"));
    }

    @Test
    public void plainValuesListsGlobsAndRanges() {
        CemNbtTest.Source flying = source("abilities.flying", "1", "1b");
        assertTrue(
            CemNbtTest.of("1")
                .test(flying, "abilities.flying"));
        assertFalse(
            CemNbtTest.of("0")
                .test(flying, "abilities.flying"));
        assertTrue(
            CemNbtTest.of("0 1")
                .test(flying, "abilities.flying"));
        assertTrue(
            CemNbtTest.of("range:0-3")
                .test(flying, "abilities.flying"));
        assertFalse(
            CemNbtTest.of("range:2-3")
                .test(flying, "abilities.flying"));
        CemNbtTest.Source name = source("CustomName", "Worker Bee", "Worker Bee");
        assertTrue(
            CemNbtTest.of("ipattern:worker*")
                .test(name, "CustomName"));
        assertTrue(
            CemNbtTest.of("\"Worker Bee\"")
                .test(name, "CustomName"));
    }

    @Test
    public void nbtCallsBecomeInputs() {
        Map<String, Double> inputs = new HashMap<>();
        CemExpression expression = CemExpressionParser.compile(
            "nbt(SelectedItem.id, raw:iregex:.*shield.*) && nbt(Tags, regex:a{1,2})",
            name -> c -> c.inputs.getOrDefault(name, 0d));
        List<String> queries = new java.util.ArrayList<>();
        for (CemNbtQueries.Query query : CemNbtQueries.all()) queries.add(query.input);
        assertTrue(queries.contains("nbt(SelectedItem.id,raw:iregex:.*shield.*)"));
        assertTrue(queries.contains("nbt(Tags,regex:a{1,2})"));
        CemExpression.Context context = new CemExpression.Context(new double[0], new HashMap<>(), inputs);
        assertEquals(0, expression.evaluate(context), 0);
        inputs.put("nbt(SelectedItem.id,raw:iregex:.*shield.*)", 1d);
        inputs.put("nbt(Tags,regex:a{1,2})", 1d);
        assertEquals(1, expression.evaluate(context), 0);
    }
}
