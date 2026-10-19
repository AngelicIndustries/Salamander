package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelHorse;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import industries.angelic.salamander.cem.client.CemBinding;
import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemBoxMesh;
import industries.angelic.salamander.cem.model.CemModel;

/** Regressions for features absent from the original spider-only fixture. */
public class CemExpansionTest {

    private static final ResourceLocation FILE = new ResourceLocation("test:optifine/cem/model.jem");

    @Test
    public void foalNeckSignalsUseBabyPivotWithoutChangingNativeGeometryOrAdultPose() throws Exception {
        ModelHorse nativeModel = new ModelHorse();
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        for (String name : new String[] { "neck", "body" }) {
            java.lang.reflect.Field field = ModelHorse.class.getDeclaredField(name);
            field.setAccessible(true);
            parts.put(name, (ModelRenderer) field.get(nativeModel));
        }
        CemBinding binding = new CemBinding(nativeModel, parts);
        CemModel model = load(
            "{\"models\":[{\"part\":\"neck\",\"attach\":true,\"animations\":[{\"var.neckY\":\"neck.ty\"}]}]}",
            binding.pivots,
            binding.parents,
            128,
            128);
        CemModel.Instance instance = model.newInstance();
        ModelRenderer neck = parts.get("neck"), body = parts.get("body");
        double rest = (4 + 16.2f) * (1.5f / 2.7272f);
        nativeModel.isChild = true;
        for (int step = 0; step <= 4; step++) {
            float rear = step / 4f;
            neck.rotationPointY = 4 - 10 * rear;
            body.rotateAngleX = -(float) Math.PI / 4 * rear;
            instance.evaluate(step, Collections.emptyMap(), pose -> binding.pose(model, pose, null, parts));
            assertEquals(rest - 4 * rear, instance.variables.get("var.neckY"), 1e-6);
            binding.refresh(model.originalParts.get("neck"), instance, null, parts);
            assertEquals(
                neck.rotationPointY,
                instance.pose[model.originalParts.get("neck").index * CemModel.STRIDE + 1],
                0);
        }
        body.rotateAngleX = 0;
        neck.rotationPointY = 11;
        instance.evaluate(5, Collections.emptyMap(), pose -> binding.pose(model, pose, null, parts));
        assertEquals(rest + 3.5, instance.variables.get("var.neckY"), 1e-6);
        nativeModel.isChild = false;
        instance.evaluate(6, Collections.emptyMap(), pose -> binding.pose(model, pose, null, parts));
        assertEquals(11, instance.variables.get("var.neckY"), 0);
    }

    @Test
    public void foalReplacementsBakeSizeOnceAndLeaveUnreplacedPartsOnTheNativeRenderPath() throws Exception {
        ModelHorse nativeModel = new ModelHorse();
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        for (String name : new String[] { "neck", "body", "head" }) {
            java.lang.reflect.Field field = ModelHorse.class.getDeclaredField(name);
            field.setAccessible(true);
            parts.put(name, (ModelRenderer) field.get(nativeModel));
        }
        CemBinding binding = new CemBinding(nativeModel, parts);
        CemModel model = load(
            "{\"models\":[{\"part\":\"body\",\"animations\":[{\"body.sy\":\"0.725\"}]},{\"part\":\"neck\"}]}",
            binding.pivots,
            binding.parents,
            128,
            128);
        CemModel.Instance instance = model.newInstance();
        CemModel.Node body = model.originalParts.get("body"), neck = model.originalParts.get("neck"),
            head = model.originalParts.get("head");
        nativeModel.isChild = true;
        instance.evaluate(1, Collections.emptyMap(), pose -> binding.pose(model, pose, null, parts));
        assertTrue(binding.bakedHorseBaby(body));
        assertTrue(binding.bakedHorseBaby(neck));
        assertFalse(binding.bakedHorseBaby(head));
        for (int pass = 0; pass < 2; pass++) {
            binding.refresh(body, instance, null, parts);
            binding.refresh(head, instance, null, parts);
            assertEquals(
                (parts.get("body").rotationPointY + 20) * .5,
                instance.pose[body.index * CemModel.STRIDE + 1],
                0);
            assertEquals(.5, instance.pose[body.index * CemModel.STRIDE + 6], 0);
            assertEquals(.725, instance.pose[body.index * CemModel.STRIDE + 7], 0);
            assertEquals(1.5f / 2.7272f, instance.pose[neck.index * CemModel.STRIDE + 6], 0);
            assertEquals(parts.get("head").rotationPointY, instance.pose[head.index * CemModel.STRIDE + 1], 0);
            assertEquals(1, instance.pose[head.index * CemModel.STRIDE + 6], 0);
        }
        nativeModel.isChild = false;
        instance.evaluate(2, Collections.emptyMap(), pose -> binding.pose(model, pose, null, parts));
        assertFalse(binding.bakedHorseBaby(body));
        assertEquals(parts.get("body").rotationPointY, instance.pose[body.index * CemModel.STRIDE + 1], 0);
        assertEquals(1, instance.pose[body.index * CemModel.STRIDE + 6], 0);
    }

    @Test
    public void bipedHeadwearFollowsAnimatedNeckWithoutOverridingItsOwnAnimations() throws Exception {
        ModelBiped nativeModel = new ModelBiped();
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", nativeModel.bipedHead);
        parts.put("headwear", nativeModel.bipedHeadwear);
        parts.put("body", nativeModel.bipedBody);
        parts.put("right_arm", nativeModel.bipedRightArm);
        parts.put("left_arm", nativeModel.bipedLeftArm);
        parts.put("right_leg", nativeModel.bipedRightLeg);
        parts.put("left_leg", nativeModel.bipedLeftLeg);
        CemBinding binding = new CemBinding(nativeModel, parts);
        CemModel model = load(
            "{\"models\":[{\"part\":\"head\",\"animations\":[{\"head.tx\":\"1\",\"head.ty\":\"0.5\",\"head.tz\":\"-2\",\"head.rx\":\"0.2\",\"head.sx\":\"1.25\"}]},{\"part\":\"headwear\",\"animations\":[{\"headwear.ry\":\"0.4\",\"headwear.visible\":\"false\"}]}]}",
            binding.pivots,
            binding.parents,
            64,
            64);
        CemModel.Instance instance = model.newInstance();
        instance.evaluate(1, Collections.emptyMap(), pose -> binding.pose(model, pose, null, parts));
        CemModel.Node headwear = model.originalParts.get("headwear");
        int offset = headwear.index * CemModel.STRIDE;
        // Native headwear still has the pre-animation position, including on subsequent eye/shader passes.
        for (int pass = 0; pass < 2; pass++) {
            binding.refresh(headwear, instance, null, parts);
            assertEquals(1, instance.pose[offset], 0);
            assertEquals(.5, instance.pose[offset + 1], 0);
            assertEquals(-2, instance.pose[offset + 2], 0);
            assertEquals(.2, instance.pose[offset + 3], 0);
            assertEquals(.4, instance.pose[offset + 4], 0);
            assertEquals(1.25, instance.pose[offset + 6], 0);
            assertEquals(0, instance.pose[offset + 9], 0);
        }
        assertEquals(0, nativeModel.bipedHeadwear.rotationPointZ, 0);
    }

    @Test
    public void namedChildrenSurviveParentReplacementAndAnimationCanResolveThem() throws Exception {
        Map<String, float[]> pivots = new LinkedHashMap<>();
        pivots.put("head", new float[] { 0, 0, 0 });
        pivots.put("nose", new float[] { 0, -2, -4 });
        pivots.put("$native", new float[] { 0, 0, 0 });
        Map<String, String> parents = new HashMap<>();
        parents.put("nose", "head");
        parents.put("$native", "head");
        CemModel model = load(
            "{\"models\":[{\"part\":\"head\",\"id\":\"replacement\"},{\"part\":\"nose\",\"submodels\":[{\"id\":\"nose2\"}],\"animations\":[{\"nose2.rx\":\"0.5\"}]}]}",
            pivots,
            parents,
            64,
            64);
        assertTrue(model.originalParts.get("head").children.contains(model.originalParts.get("nose")));
        assertFalse(model.originalParts.get("head").children.contains(model.originalParts.get("$native")));
        CemModel.Instance instance = model.newInstance();
        instance.evaluate(1, Collections.emptyMap(), pose -> {});
        assertEquals(.5, instance.pose[model.find("head:nose:nose2", null, null).index * CemModel.STRIDE + 3], 0);
    }

    @Test
    public void nonFiniteAnimationHidesOnlyItsNodeEvenAfterNativePoseRefresh() throws Exception {
        CemModel model = load(
            "{\"models\":[{\"part\":\"head\",\"animations\":[{\"head.rx\":\"1/0\"}]}]}",
            Collections.singletonMap("head", new float[] { 0, 0, 0 }),
            Collections.emptyMap(),
            64,
            32);
        CemModel.Instance pose = model.newInstance();
        pose.evaluate(1, Collections.emptyMap(), v -> {});
        CemModel.Node head = model.originalParts.get("head");
        pose.nativePose(head, new float[] { 1, 2, 3, 4, 5, 6 }, true);
        assertEquals(0, pose.pose[head.index * CemModel.STRIDE + 9], 0);
        assertEquals(1, pose.pose[9], 0);
        for (double value : pose.pose) assertTrue(Double.isFinite(value));
    }

    @Test
    public void missingTextureSizeUsesAdapterDimensionsAndPerFaceUvKeepsSignedCoordinates() throws Exception {
        CemModel model = load(
            "{\"models\":[{\"part\":\"head\",\"id\":\"surface\",\"boxes\":[{\"coordinates\":[0,0,0,-2,4,0],\"uvNorth\":[16,8,0,0]}]}]}",
            Collections.singletonMap("head", new float[] { 0, 0, 0 }),
            Collections.emptyMap(),
            128,
            64);
        CemModel.Box box = model.find("surface", null, null).boxes.get(0);
        assertEquals(128, box.textureWidth);
        assertEquals(64, box.textureHeight);
        CemBoxMesh mesh = new CemBoxMesh(box);
        int count = 0;
        for (float[] face : mesh.faces) if (face != null) {
            count++;
            for (float value : face) assertTrue(Float.isFinite(value));
        }
        assertEquals(1, count);
        assertEquals(0, mesh.faces[4][3], 0);
        assertEquals(8 / 64f, mesh.faces[4][4], 0);
    }

    @Test
    public void attachmentsResolveRelativeToImportedFileAndKeepBothHands() throws Exception {
        Map<ResourceLocation, String> files = new HashMap<>();
        files.put(FILE, "{\"models\":[{\"part\":\"head\",\"model\":\"./parts/arm.jpm\"}]}");
        files.put(
            new ResourceLocation("test:optifine/cem/parts/arm.jpm"),
            "{\"id\":\"arm\",\"texture\":\"./skin.png\",\"invertAxis\":\"xy\",\"attachments\":{\"left_handheld_item\":[1,2,3],\"right_handheld_item\":[4,5,6]}}");
        CemModel model = new CemLoader(location -> new StringReader(files.get(location)))
            .load(FILE, Collections.singletonMap("head", new float[] { 0, 0, 0 }));
        CemModel.Node arm = model.find("arm", null, null);
        assertEquals("test:optifine/cem/parts/skin.png", arm.texture.toString());
        assertArrayEquals(new float[] { -1, -2, 3 }, arm.attachments.get("left_handheld_item"), 0);
        assertArrayEquals(new float[] { -4, -5, 6 }, arm.attachments.get("right_handheld_item"), 0);
    }

    @Test
    public void layersShareEntityVariablesWithoutResettingBodyRenderProperties() throws Exception {
        Map<String, float[]> pivots = Collections.singletonMap("head", new float[] { 0, 0, 0 });
        CemModel body = load(
            "{\"models\":[{\"part\":\"head\",\"animations\":[{\"var.counter\":\"var.counter+1\",\"render.shadow_size\":\"2\",\"render.leash_offset_y\":\"0.75\"}]}]}",
            pivots,
            Collections.emptyMap(),
            64,
            32);
        CemModel layer = load(
            "{\"models\":[{\"part\":\"head\",\"animations\":[{\"head.rx\":\"var.counter\"}]}]}",
            pivots,
            Collections.emptyMap(),
            64,
            32);
        Map<String, Double> variables = new HashMap<>();
        CemModel.Instance bodyPose = body.newInstance(variables), layerPose = layer.newInstance(variables);
        bodyPose.evaluate(1, Collections.emptyMap(), p -> {});
        layerPose.evaluate(1, Collections.emptyMap(), p -> {});
        bodyPose.evaluate(1, Collections.emptyMap(), p -> fail("Eye passes must reuse this frame's pose"));
        assertEquals(1, variables.get("var.counter"), 0);
        assertEquals(1, layerPose.pose[layer.originalParts.get("head").index * CemModel.STRIDE + 3], 0);
        assertEquals(2, bodyPose.renderProperties.get("render.shadow_size"), 0);
        assertEquals(.75, bodyPose.renderProperties.get("render.leash_offset_y"), 0);
        assertEquals(0, layerPose.renderProperties.get("render.leash_offset_y"), 0);
    }

    private CemModel load(String json, Map<String, float[]> pivots, Map<String, String> parents, int width, int height)
        throws Exception {
        return new CemLoader(location -> new StringReader(json)).load(FILE, pivots, parents, width, height);
    }
}
