package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

public class CemLoaderTest {

    private static final ResourceLocation SOURCE = new ResourceLocation("minecraft", "optifine/cem/spider.jem");

    @Test
    public void crossResourceAnimationsBindLaterPartsAndPreserveFrameState() throws Exception {
        Map<ResourceLocation, String> files = new HashMap<>();
        files.put(
            SOURCE,
            "{\"models\":[{\"part\":\"root\",\"model\":\"animation.jpm\",\"attach\":\"true\"},"
                + "{\"part\":\"head\",\"id\":\"custom\",\"invertAxis\":\"xy\",\"translate\":[2,3,4],"
                + "\"boxes\":[{\"textureOffset\":[0,0],\"coordinates\":[1,2,3,4,5,6]}],"
                + "\"animations\":[{\"head.rx\":\"head.rx+torad(10)\"}]}]}");
        files.put(
            CemLoader.path(SOURCE, "animation.jpm", ".jpm"),
            "{\"animations\":[{\"var.counter\":\"var.counter+1\",\"custom.tx\":\"var.counter\",\"head.rx\":\"torad(20)\"}]}");
        CemModel model = load(files);
        assertEquals(4, model.assignmentCount());
        CemModel.Node custom = model.find("custom", null, null);
        assertEquals(-2, custom.transform[0], 0);
        assertArrayEquals(new float[] { -5, -7, 3, 4, 5, 6 }, custom.boxes.get(0).coordinates, 0);
        assertFalse(model.originalParts.get("head").vanillaGeometry);
        CemModel.Instance first = model.newInstance();
        CemModel.Instance second = model.newInstance();
        first.evaluate(10, Collections.emptyMap(), pose -> {});
        first.evaluate(10, Collections.emptyMap(), pose -> fail("same frame must reuse pose"));
        assertEquals(1, first.variables.get("var.counter"), 0);
        assertEquals(Math.toRadians(30), first.pose[model.originalParts.get("head").index * CemModel.STRIDE + 3], 1e-8);
        first.evaluate(11, Collections.emptyMap(), pose -> {});
        second.evaluate(11, Collections.emptyMap(), pose -> {});
        assertEquals(2, first.variables.get("var.counter"), 0);
        assertEquals(1, second.variables.get("var.counter"), 0);
        assertEquals(2, first.pose[custom.index * CemModel.STRIDE], 0);
    }

    @Test
    public void replacementsRetainEveryCustomSiblingIncludingEarlierAttachments() throws Exception {
        Map<ResourceLocation, String> files = new HashMap<>();
        files.put(
            SOURCE,
            "{\"models\":[" + "{\"part\":\"head\",\"id\":\"attached\",\"attach\":true},"
                + "{\"part\":\"head\",\"id\":\"face\"},"
                + "{\"part\":\"head\",\"id\":\"skull\"}]}");
        CemModel model = load(files);
        CemModel.Node head = model.originalParts.get("head");
        assertFalse(head.vanillaGeometry);
        assertEquals(3, head.children.size());
        assertSame(model.find("face", null, null), head.children.get(1));
        assertSame(model.find("skull", null, null), head.children.get(2));
    }

    @Test
    public void staticHandPoseUsesNativeTransformsWithoutRunningBodyAnimations() throws Exception {
        Map<ResourceLocation, String> files = new HashMap<>();
        files.put(
            SOURCE,
            "{\"models\":[{\"part\":\"head\",\"animations\":[{\"head.rx\":\"2\",\"var.counter\":\"var.counter+1\"}]}]}");
        CemModel model = load(files);
        CemModel.Instance instance = model.newInstance();
        int rotation = model.originalParts.get("head").index * CemModel.STRIDE + 3;
        instance.staticPose(pose -> pose[rotation] = .5);
        assertEquals(.5, instance.pose[rotation], 0);
        assertTrue(instance.variables.isEmpty());
        assertFalse(instance.evaluated(1));
        instance.evaluate(1, Collections.emptyMap(), pose -> {});
        assertEquals(2, instance.pose[rotation], 0);
        assertEquals(1, instance.variables.get("var.counter"), 0);
    }

    @Test
    public void missingReferencesKeepTheirOwnBoxes() throws Exception {
        Map<ResourceLocation, String> files = new HashMap<>();
        files.put(
            SOURCE,
            "{\"models\":[{\"part\":\"head\",\"id\":\"head\",\"boxes\":[{\"textureOffset\":[0,0],\"coordinates\":[-4,0,-4,8,8,8]}],"
                + "\"submodels\":[{\"id\":\"face\",\"model\":\"face.jpm\"}]}]}");
        CemLoader loader = loader(files);
        CemModel model = loader.load(SOURCE, Collections.singletonMap("head", new float[] { 0, 15, -3 }));
        assertNotNull(model.originalParts.get("head"));
        assertEquals(Collections.singletonList(CemLoader.path(SOURCE, "face.jpm", ".jpm")), loader.missingReferences());

        files.put(CemLoader.path(SOURCE, "face.jpm", ".jpm"), "{\"boxes\":[");
        assertThrows(IOException.class, () -> load(files));
    }

    @Test
    public void detectsMissingCyclicAndUnboundResources() {
        Map<ResourceLocation, String> files = new HashMap<>();
        files.put(SOURCE, "{\"models\":[{\"part\":\"head\",\"model\":\"a.jpm\"}]}");
        files.put(CemLoader.path(SOURCE, "a.jpm", ".jpm"), "{\"model\":\"a.jpm\"}");
        assertThrows(IOException.class, () -> load(files));
        files.put(SOURCE, "{\"models\":[{\"part\":\"head\",\"animations\":[{\"missing.rx\":\"1\"}]}]}");
        assertThrows(IllegalArgumentException.class, () -> load(files));
    }

    @Test
    public void pathsHonorNamespacesRelativeAndOptifineRoots() {
        assertEquals(
            "minecraft:optifine/cem/a.jpm",
            CemLoader.path(SOURCE, "a", ".jpm")
                .toString());
        assertEquals(
            "minecraft:optifine/other.png",
            CemLoader.path(SOURCE, "~/other", ".png")
                .toString());
        assertEquals(
            "example:textures/a.png",
            CemLoader.path(SOURCE, "example:textures/a", ".png")
                .toString());
        assertEquals(
            "minecraft:optifine/b.jpm",
            CemLoader.path(SOURCE, "./../b", ".jpm")
                .toString());
        assertThrows(IllegalArgumentException.class, () -> CemLoader.path(SOURCE, "./../../../escape", ".jpm"));
    }

    private CemModel load(Map<ResourceLocation, String> files) throws IOException {
        return loader(files).load(SOURCE, Collections.singletonMap("head", new float[] { 0, 15, -3 }));
    }

    /** Missing files fail as the game's resource manager fails them. */
    private CemLoader loader(Map<ResourceLocation, String> files) {
        return new CemLoader(location -> {
            String contents = files.get(location);
            if (contents == null) throw new FileNotFoundException(location.toString());
            return new StringReader(contents);
        });
    }
}
