package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

import org.junit.Assume;
import org.junit.Test;

import industries.angelic.salamander.cem.client.CemBinding;
import industries.angelic.salamander.cem.client.CemPlayers;
import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

/** Original archives stay external. Exercise both player layouts and the body/accessory variable contract. */
public class FreshMovesPackTest {

    @Test
    public void originalPacksAnimateBodiesOverlaysAndAccessoriesAcrossMovementStates() throws Exception {
        String directory = System.getenv("SALAMANDER_CEM_PACK_DIR");
        Assume.assumeNotNull(directory);
        for (String version : new String[] { "-1.21.2 Fresh Moves v3.1", "+1.21.3 Fresh Moves v3.1.1" }) {
            for (String eyes : new String[] { "With", "No" }) {
                File file = new File(directory, version + " (" + eyes + " Animated Eyes).zip");
                Assume.assumeTrue(file.isFile());
                try (ZipFile zip = new ZipFile(file)) {
                    for (String target : new String[] { "player", "player_slim" }) {
                        ModelBiped nativeModel = new ModelBiped();
                        CemPlayers.register(nativeModel);
                        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
                        parts.put("head", nativeModel.bipedHead);
                        parts.put("headwear", nativeModel.bipedHeadwear);
                        parts.put("body", nativeModel.bipedBody);
                        parts.put("right_arm", nativeModel.bipedRightArm);
                        parts.put("left_arm", nativeModel.bipedLeftArm);
                        parts.put("right_leg", nativeModel.bipedRightLeg);
                        parts.put("left_leg", nativeModel.bipedLeftLeg);
                        parts.put("cloak", nativeModel.bipedCloak);
                        CemBinding binding = new CemBinding(nativeModel, parts);
                        CemModel body = loader(zip).load(path(target), binding.pivots, binding.parents, 64, 64);
                        assertTrue(body.assignmentCount() > 70);
                        assertTrue(body.root.children.contains(body.originalParts.get("right_sleeve")));
                        Map<String, Double> variables = new HashMap<>();
                        CemModel.Instance instance = body.newInstance(variables);
                        Map<String, Double> inputs = new HashMap<>();
                        inputs.put("id", 42d);
                        inputs.put("frame_time", 1d / 60);
                        double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
                        for (int frame = 0; frame < 600; frame++) {
                            int state = frame / 100;
                            inputs.put("age", frame / 3d);
                            inputs.put("limb_swing", frame / 4d);
                            inputs.put("limb_speed", state == 0 ? 0d : state == 2 ? .9d : .4d);
                            inputs.put("is_sprinting", state == 2 ? 1d : 0d);
                            inputs.put("is_sneaking", state == 3 ? 1d : 0d);
                            inputs.put("is_on_ground", state < 4 ? 1d : 0d);
                            inputs.put("is_climbing", state == 4 ? 1d : 0d);
                            inputs.put("is_gliding", state == 5 ? 1d : 0d);
                            nativeModel.bipedRightArm.rotateAngleX = -.8f;
                            instance.evaluate(frame, inputs, pose -> binding.pose(body, pose, null, parts));
                            binding.playerLayers(instance, false);
                            assertEquals(-.8, value(body, instance, "right_arm", 3), 1e-6);
                            for (String[] pair : new String[][] { { "body", "jacket" }, { "right_arm", "right_sleeve" },
                                { "left_leg", "left_pants" } })
                                for (int axis = 0; axis < 6; axis++) assertEquals(
                                    file.getName() + " " + pair[1],
                                    value(body, instance, pair[0], axis),
                                    value(body, instance, pair[1], axis),
                                    1e-6);
                            double y = value(body, instance, "body", 1);
                            minimum = Math.min(minimum, y);
                            maximum = Math.max(maximum, y);
                            for (double value : instance.pose) assertTrue(Double.isFinite(value));
                        }
                        assertTrue("Body should move", maximum - minimum > 2);
                        for (String accessory : new String[] { "player_cape", "elytra" }) {
                            Map<String, float[]> pivots = new LinkedHashMap<>();
                            for (String part : new String[] { "cloak", "cape", "right_wing", "left_wing" })
                                pivots.put(part, new float[] { 0, 0, 0 });
                            CemModel model = loader(zip).load(path(accessory), pivots);
                            CemModel.Instance accessoryPose = model.newInstance(variables);
                            accessoryPose.evaluate(601, inputs, pose -> {});
                            for (double value : accessoryPose.pose) assertTrue(Double.isFinite(value));
                            if (accessory.equals("elytra")) assertEquals(
                                variables.get("var.player_body_ty") + 1,
                                value(model, accessoryPose, "right_wing", 1),
                                1e-6);
                        }
                        // Hands skip body animations but independently positioned sleeves must still follow.
                        instance.staticPose(pose -> binding.pose(body, pose, null, parts));
                        binding.playerLayers(instance, true);
                        assertEquals(-5, value(body, instance, "right_sleeve", 0), 0);
                        assertEquals(2, value(body, instance, "right_sleeve", 1), 0);
                    }
                }
            }
        }
    }

    private static double value(CemModel model, CemModel.Instance pose, String part, int axis) {
        return pose.pose[model.originalParts.get(part).index * CemModel.STRIDE + axis];
    }

    private static ResourceLocation path(String name) {
        return new ResourceLocation("optifine/cem/" + name + ".jem");
    }

    private static CemLoader loader(ZipFile zip) {
        return new CemLoader(location -> {
            ZipEntry entry = zip.getEntry("assets/" + location.getResourceDomain() + "/" + location.getResourcePath());
            if (entry == null) throw new FileNotFoundException(location.toString());
            return new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8);
        });
    }
}
