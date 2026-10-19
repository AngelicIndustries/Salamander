package industries.angelic.salamander.cem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

import industries.angelic.salamander.cem.animation.CemNbtQueries;
import industries.angelic.salamander.cem.client.CemBinding;
import industries.angelic.salamander.cem.client.CemPlayers;
import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

/** The original FA+Player archive stays external. It uses EMF's variables and nbt() throughout. */
public class FreshAnimationsPlayerPackTest {

    @Test
    public void originalPackCompilesAndAnimatesThroughHeldItemsAndSwimming() throws Exception {
        String directory = System.getenv("SALAMANDER_CEM_PACK_DIR");
        Assume.assumeNotNull(directory);
        File file = new File(directory, "FA+Player-v1.1.zip");
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
                CemModel body = new CemLoader(location -> {
                    ZipEntry entry = zip
                        .getEntry("assets/" + location.getResourceDomain() + "/" + location.getResourcePath());
                    if (entry == null) throw new FileNotFoundException(location.toString());
                    return new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8);
                }).load(new ResourceLocation("emf/cem/" + target + ".jem"), binding.pivots, binding.parents, 64, 64);
                assertTrue(body.assignmentCount() > 100);
                // The pack replaces root with animation-only parts; the body parts must stay in the tree.
                for (String part : new String[] { "head", "body", "right_arm", "left_arm", "right_leg", "left_leg" }) {
                    CemModel.Node node = body.originalParts.get(part);
                    assertTrue(target + " " + part + " reachable", reachable(body.root, node));
                    assertTrue(target + " " + part + " has boxes", boxes(node) > 0);
                }
                String shield = "nbt(SelectedItem.id,raw:iregex:.*shield.*)";
                boolean registered = false;
                for (CemNbtQueries.Query query : CemNbtQueries.all()) registered |= query.input.equals(shield);
                assertTrue(registered);

                CemModel.Instance instance = body.newInstance(new HashMap<>());
                Map<String, Double> inputs = new HashMap<>();
                inputs.put("id", 42d);
                inputs.put("frame_time", 1d / 60);
                inputs.put("is_right_handed", 1d);
                for (int frame = 0; frame < 600; frame++) {
                    int state = frame / 100;
                    inputs.put("frame_counter", (double) frame);
                    inputs.put("age", frame / 3d);
                    inputs.put("limb_swing", frame / 4d);
                    inputs.put("limb_speed", state == 0 ? 0d : .6d);
                    inputs.put("is_on_ground", state == 4 ? 0d : 1d);
                    inputs.put("is_holding_item_right", state >= 1 ? 1d : 0d);
                    inputs.put("is_using_item", state == 2 ? 1d : 0d);
                    inputs.put("is_swinging_right_arm", state == 2 ? 1d : 0d);
                    inputs.put(shield, state == 2 ? 1d : 0d);
                    inputs.put("is_jumping", state == 3 ? 1d : 0d);
                    inputs.put("is_in_water", state == 4 ? 1d : 0d);
                    inputs.put("fluid_depth_up", state == 4 ? 3d : 0d);
                    inputs.put("is_swimming", state == 4 ? 1d : 0d);
                    inputs.put("is_paused", state == 5 && frame % 2 == 0 ? 1d : 0d);
                    instance.evaluate(frame, inputs, pose -> binding.pose(body, pose, null, parts));
                    for (double value : instance.pose) assertTrue(target + " frame " + frame, Double.isFinite(value));
                    if (state == 0)
                        for (String part : new String[] { "head", "body", "right_arm", "left_leg" }) assertEquals(
                            target + " " + part + " visible",
                            1,
                            instance.pose[body.originalParts.get(part).index * CemModel.STRIDE + 9],
                            0);
                }
            }
        }
    }

    private static boolean reachable(CemModel.Node from, CemModel.Node node) {
        if (from == node) return true;
        for (CemModel.Node child : from.children) if (reachable(child, node)) return true;
        return false;
    }

    private static int boxes(CemModel.Node node) {
        int count = node.boxes.size();
        for (CemModel.Node child : node.children) count += boxes(child);
        return count;
    }
}
