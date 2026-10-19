package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.util.ResourceLocation;

import org.junit.Assume;
import org.junit.Test;

import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

/** External fixtures: download the original pinned packs; never bundle their assets into the test jar. */
public class FreshSpidersPackTest {

    @Test
    public void compilesAndRunsBothUnmodifiedPacksAcrossManyFrames() throws Exception {
        String directory = System.getenv("SALAMANDER_CEM_PACK_DIR");
        Assume.assumeNotNull(directory);
        try (ZipFile extension = new ZipFile(new File(directory, "FA+Spiders-v2.2.zip"));
            ZipFile base = new ZipFile(new File(directory, "FreshAnimations_v1.10.5.zip"))) {
            for (String name : new String[] { "spider", "cave_spider" }) {
                CemModel model = new CemLoader(location -> {
                    String path = "assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
                    ZipEntry entry = extension.getEntry(path);
                    ZipFile pack = extension;
                    if (entry == null) {
                        entry = base.getEntry(path);
                        pack = base;
                    }
                    if (entry == null) throw new IOException("Missing " + path);
                    return new InputStreamReader(pack.getInputStream(entry), StandardCharsets.UTF_8);
                }).load(new ResourceLocation("minecraft", "optifine/cem/" + name + ".jem"), spiderPivots());
                assertEquals(134, model.assignmentCount());
                assertEquals(
                    name.equals("spider") ? 25 : 23,
                    model.nodes.stream()
                        .mapToInt(n -> n.boxes.size())
                        .sum());
                CemModel.Instance instance = model.newInstance();
                Map<String, Double> inputs = new HashMap<>();
                inputs.put("id", 42d);
                inputs.put("frame_time", 1d / 60);
                for (int frame = 1; frame <= 360; frame++) {
                    inputs.put("frame_counter", (double) frame);
                    inputs.put("age", frame / 3d);
                    inputs.put("is_alive", frame < 300 ? 1d : 0d);
                    inputs.put("is_hurt", frame % 40 < 10 ? 1d : 0d);
                    inputs.put("hurt_time", (double) Math.max(0, 10 - frame % 40));
                    inputs.put("is_on_ground", frame % 120 < 90 ? 1d : 0d);
                    inputs.put("is_aggressive", frame > 120 ? 1d : 0d);
                    inputs.put("limb_swing", frame / 4d);
                    inputs.put("limb_speed", Math.abs(Math.sin(frame / 20d)));
                    inputs.put("swing_progress", frame % 30 < 6 ? frame % 30 / 6d : 0);
                    inputs.put("head_yaw", Math.sin(frame / 12d) * 90);
                    inputs.put("head_pitch", Math.cos(frame / 15d) * 45);
                    inputs.put("rot_y", Math.sin(frame / 30d) * Math.PI);
                    instance.evaluate(frame, inputs, pose -> {});
                    double[] pose = instance.pose.clone();
                    instance.evaluate(frame, inputs, ignored -> fail("Duplicate render advanced animation"));
                    assertArrayEquals(pose, instance.pose, 0);
                    for (double value : pose) assertTrue(Double.isFinite(value));
                }
            }
        }
    }

    public static Map<String, float[]> spiderPivots() {
        Map<String, float[]> pivots = new LinkedHashMap<>();
        pivots.put("head", new float[] { 0, 15, -3 });
        pivots.put("neck", new float[] { 0, 15, 0 });
        pivots.put("body", new float[] { 0, 15, 9 });
        for (int i = 1; i <= 8; i++) pivots.put("leg" + i, new float[] { i % 2 == 1 ? -4 : 4, 15, 2 - (i - 1) / 2 });
        return pivots;
    }
}
