package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.util.ResourceLocation;

import org.junit.Assume;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

/** Exercises the format and expression corpus, independently of which entities the game provides. */
public class FreshAnimationsFormatTest {

    @Test
    public void evaluatesEveryUnmodifiedModel() throws Exception {
        String directory = System.getenv("SALAMANDER_CEM_PACK_DIR");
        Assume.assumeNotNull(directory);
        try (ZipFile zip = new ZipFile(new File(directory, "FreshAnimations_v1.10.5.zip"))) {
            int count = 0;
            for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName()
                    .endsWith(".jem")) continue;
                JsonObject doc;
                try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                    doc = new JsonParser().parse(reader)
                        .getAsJsonObject();
                }
                Map<String, float[]> parts = new LinkedHashMap<>();
                for (JsonElement model : doc.getAsJsonArray("models")) parts.put(
                    model.getAsJsonObject()
                        .get("part")
                        .getAsString(),
                    new float[] { 0, 0, 0 });
                CemModel model = new CemLoader(location -> {
                    ZipEntry resource = zip
                        .getEntry("assets/" + location.getResourceDomain() + "/" + location.getResourcePath());
                    if (resource == null) throw new FileNotFoundException(location.toString());
                    return new InputStreamReader(zip.getInputStream(resource), StandardCharsets.UTF_8);
                }).load(
                    new ResourceLocation(
                        "minecraft",
                        entry.getName()
                            .substring("assets/minecraft/".length())),
                    parts);
                CemModel.Instance instance = model.newInstance();
                Map<String, Double> inputs = new HashMap<>();
                inputs.put("id", 42d);
                inputs.put("frame_time", 1d / 60);
                inputs.put("health", 20d);
                inputs.put("max_health", 20d);
                for (int frame = 1; frame <= 180; frame++) {
                    inputs.put("frame_counter", (double) frame);
                    inputs.put("age", frame / 3d);
                    inputs.put("is_alive", 1d);
                    inputs.put("is_on_ground", 1d);
                    inputs.put("limb_speed", Math.abs(Math.sin(frame / 20d)));
                    inputs.put("limb_swing", frame / 4d);
                    inputs.put("head_yaw", Math.sin(frame / 12d) * 45);
                    inputs.put("head_pitch", Math.cos(frame / 15d) * 30);
                    inputs.put("is_aggressive", frame > 90 ? 1d : 0d);
                    inputs.put("is_child", frame > 120 ? 1d : 0d);
                    inputs.put("rule_index", frame > 150 ? 2d : 0d);
                    try {
                        instance.evaluate(frame, inputs, pose -> {});
                    } catch (Exception error) {
                        throw new AssertionError(entry.getName() + " frame " + frame, error);
                    }
                    for (double value : instance.pose) assertTrue(entry.getName(), Double.isFinite(value));
                }
                count++;
            }
            assertEquals(114, count);
        }
    }
}
