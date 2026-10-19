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

import net.minecraft.util.ResourceLocation;

import org.junit.Assume;
import org.junit.Test;

import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

/** The original pack stays outside the repository. This checks geometry reachability as well as expressions. */
public class JustExpressionsPackTest {

    @Test
    public void originalWideAndSlimFacesRemainAttachedAndAnimate() throws Exception {
        String directory = System.getenv("SALAMANDER_CEM_PACK_DIR");
        Assume.assumeNotNull(directory);
        File pack = new File(directory, "JustExpressions_v1.2.1.zip");
        Assume.assumeTrue(pack.isFile());
        try (ZipFile zip = new ZipFile(pack)) {
            for (String name : new String[] { "player", "player_slim" }) {
                Map<String, float[]> parts = new LinkedHashMap<>();
                for (String part : new String[] { "head", "headwear", "body", "jacket", "right_arm", "left_arm",
                    "right_sleeve", "left_sleeve", "right_leg", "left_leg", "right_pants", "left_pants" })
                    parts.put(part, new float[] { 0, 0, 0 });
                CemModel model = new CemLoader(location -> {
                    ZipEntry entry = zip
                        .getEntry("assets/" + location.getResourceDomain() + "/" + location.getResourcePath());
                    if (entry == null) throw new FileNotFoundException(location.toString());
                    return new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8);
                }).load(new ResourceLocation("emf/cem/" + name + ".jem"), parts);
                CemModel.Node face = model.find("player_face", null, null);
                assertNotNull(face);
                assertTrue(model.originalParts.get("head").children.contains(face));
                assertTrue(model.assignmentCount() > 200);
                CemModel.Node pupil = model.find("r_pupil1", null, null);
                CemModel.Instance instance = model.newInstance();
                Map<String, Double> inputs = new HashMap<>();
                inputs.put("id", 42d);
                double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
                for (int frame = 0; frame < 600; frame++) {
                    inputs.put("age", frame / 3d);
                    inputs.put("head_yaw", Math.sin(frame / 30d) * 60);
                    inputs.put("head_pitch", Math.cos(frame / 30d) * 45);
                    inputs.put("distance", frame / 5d);
                    inputs.put("is_hurt", frame < 30 ? 1d : 0d);
                    inputs.put("hurt_time", Math.max(0, 10 - frame / 3d));
                    instance.evaluate(frame, inputs, pose -> {});
                    double x = instance.pose[pupil.index * CemModel.STRIDE];
                    minimum = Math.min(minimum, x);
                    maximum = Math.max(maximum, x);
                    for (double value : instance.pose) assertTrue(name, Double.isFinite(value));
                }
                assertTrue("Pupils should move", maximum - minimum > .5);
                assertEquals(
                    1 + (599d / 5 / 1000) * 500,
                    instance.pose[model.find("2by1", null, null).index * CemModel.STRIDE + 8],
                    1e-6);
            }
        }
    }
}
