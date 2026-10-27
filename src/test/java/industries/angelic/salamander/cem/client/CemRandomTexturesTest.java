package industries.angelic.salamander.cem.client;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import industries.angelic.salamander.cem.loading.CemRules;
import industries.angelic.salamander.cem.loading.CemTextureSet;
import industries.angelic.salamander.config.TextureConfig;

public class CemRandomTexturesTest {

    private static final ResourceLocation BASE = new ResourceLocation("textures/entity/test/test.png");
    private final Resources resources = new Resources();
    private boolean enabled;
    private long frame;

    @Before
    public void before() throws Exception {
        enabled = TextureConfig.randomEntityTextures;
        frame = CemClient.frame();
        TextureConfig.randomEntityTextures = true;
        CemRandomTextures.reload(null);
        CemRuleFacts.clear();
        field(CemRandomTextures.class, "resources").set(null, resources);
    }

    @After
    public void after() throws Exception {
        CemRandomTextures.reload(null);
        field(CemRandomTextures.class, "resources").set(null, null);
        CemRuleFacts.clear();
        field(CemRuntime.class, "subject").set(null, null);
        field(CemClient.class, "frame").setLong(null, frame);
        TextureConfig.randomEntityTextures = enabled;
    }

    @Test
    public void ruleFreeTexturesDoNotRebuildFactsOnLaterTicks() throws Exception {
        Resources r = resources.add(BASE)
            .add(id("optifine/random/entity/test/test2.png"));
        install(BASE, CemTextureSet.load(Collections.singletonList(BASE), r));
        TestTile tile = new TestTile();
        ResourceLocation selected = resolve(tile);
        Object initial = facts().get(tile);
        assertNotNull(initial);
        for (int i = 0; i < 100; i++) {
            nextTick();
            assertEquals(selected, resolve(tile));
            assertSame(
                "No per-tick facts allocation or world sampling for numbered textures",
                initial,
                facts().get(tile));
        }
    }

    @Test
    public void dynamicRulesRefreshButInitialHeightRemainsFrozen() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("textures.1", "2");
        properties.setProperty("heights.1", "10");
        properties.setProperty("nbt.1.Marker", "a");
        properties.setProperty("textures.2", "3");
        ResourceLocation rules = id("optifine/random/entity/test/test.properties");
        resources.add(BASE)
            .add(rules)
            .add(id("optifine/random/entity/test/test2.png"))
            .add(id("optifine/random/entity/test/test3.png"));
        resources.properties.put(rules, properties);
        install(BASE, CemTextureSet.load(Collections.singletonList(BASE), resources));
        TestTile tile = new TestTile();
        tile.yCoord = 10;
        assertEquals(id("optifine/random/entity/test/test2.png"), resolve(tile));
        Object initial = facts().get(tile);
        nextTick();
        tile.yCoord = 40;
        tile.marker = "b";
        assertEquals(id("optifine/random/entity/test/test3.png"), resolve(tile));
        assertNotSame(initial, facts().get(tile));
        nextTick();
        tile.marker = "a";
        assertEquals(id("optifine/random/entity/test/test2.png"), resolve(tile));
        CemRandomTextures.clearEntities();
        CemRuleFacts.clear();
        assertEquals("World clear discards frozen height", id("optifine/random/entity/test/test3.png"), resolve(tile));
    }

    @Test
    public void sparseOverlayInheritanceStillChangesOnNextTick() throws Exception {
        resources.add(BASE)
            .add(id("optifine/random/entity/test/test5.png"));
        install(BASE, CemTextureSet.load(Collections.singletonList(BASE), resources));
        TestTile tile = new TestTile();
        begin(tile);
        CemRuntime.textureChoice(new CemRules.Selection(5, 7));
        assertEquals(id("optifine/random/entity/test/test5.png"), CemRandomTextures.resolve(BASE));
        nextTick();
        begin(tile);
        CemRuntime.textureChoice(new CemRules.Selection(1, 0));
        assertEquals(BASE, CemRandomTextures.resolve(BASE));
    }

    @Test
    public void resourceReloadDropsSelectionsAndInitialFacts() throws Exception {
        resources.add(BASE);
        install(BASE, CemTextureSet.load(Collections.singletonList(BASE), resources));
        TestTile tile = new TestTile();
        assertEquals(BASE, resolve(tile));
        Object initial = facts().get(tile);
        CemRandomTextures.reload(null);
        field(CemRandomTextures.class, "resources").set(null, resources);
        resources.add(id("optifine/random/entity/test/test2.png"));
        install(BASE, CemTextureSet.load(Collections.singletonList(BASE), resources));
        tile.yCoord = 1;
        nextTick();
        begin(tile);
        CemRuntime.textureChoice(new CemRules.Selection(2, 0));
        assertEquals(id("optifine/random/entity/test/test2.png"), CemRandomTextures.resolve(BASE));
        assertNotSame(initial, facts().get(tile));
    }

    @Test
    public void defaultTextureDoesNotReadFactsDuringSelection() throws Exception {
        CemTextureSet set = CemTextureSet.load(Collections.singletonList(BASE), resources.add(BASE));
        CemRules.Selection selected = set.select(null, null);
        assertEquals(1, selected.model);
        assertEquals(0, selected.rule);
        CemRules.Selection inherited = new CemRules.Selection(9, 4);
        assertSame(inherited, set.select(null, inherited));
    }

    private static void begin(Object object) throws Exception {
        field(CemRuntime.class, "subject").set(null, new CemRuntime.Subject(object, 0, null));
    }

    private static ResourceLocation resolve(Object object) throws Exception {
        begin(object);
        return CemRandomTextures.resolve(BASE);
    }

    private static void nextTick() throws Exception {
        field(CemClient.class, "frame").setLong(null, CemClient.frame() + 1);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> facts() throws Exception {
        return (Map<Object, Object>) field(CemRuleFacts.class, "CACHE").get(null);
    }

    @SuppressWarnings("unchecked")
    private static void install(ResourceLocation base, CemTextureSet set) throws Exception {
        ((Map<ResourceLocation, CemTextureSet>) field(CemRandomTextures.class, "SETS").get(null)).put(base, set);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(path);
    }

    private static final class TestTile extends TileEntity {

        String marker = "a";

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            tag.setString("Marker", marker);
        }
    }

    private static final class Resources implements CemTextureSet.Resources {

        final Map<ResourceLocation, Properties> properties = new HashMap<>();
        final java.util.Set<ResourceLocation> locations = new java.util.HashSet<>();

        Resources add(ResourceLocation location) {
            locations.add(location);
            return this;
        }

        @Override
        public boolean exists(ResourceLocation location) {
            return locations.contains(location);
        }

        @Override
        public int priority(ResourceLocation location) {
            return exists(location) ? 0 : -1;
        }

        @Override
        public Properties properties(ResourceLocation location) {
            return properties.get(location);
        }
    }
}
