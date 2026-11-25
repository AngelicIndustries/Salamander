package industries.angelic.salamander.cem.client;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import industries.angelic.salamander.api.cem.CemApi;
import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;
import industries.angelic.salamander.config.CemConfig;

public class CemApiTest {

    public static double triple(double value) {
        return value * 3;
    }

    public static double mix(double a, double b) {
        return a * 10 + b;
    }

    @Test
    public void registeredVariablesAndFunctionsAnimateCompiledAndInterpreted() throws Exception {
        CemApi.registerVariable("test", "test_wave", subject -> 0.25);
        CemApi.registerFunction("test", "test_triple", CemApiTest.class.getMethod("triple", double.class));
        CemApi.registerFunction("test", "test_mix", CemApiTest.class.getMethod("mix", double.class, double.class));
        boolean compiled = CemConfig.compiledAnimations;
        try {
            for (boolean compile : new boolean[] { true, false }) {
                CemConfig.compiledAnimations = compile;
                CemModel model = new CemLoader(
                    location -> new StringReader(
                        "{\"models\":[{\"part\":\"head\",\"animations\":[{\"head.rx\":\"test_triple(test_wave)\","
                            + "\"head.ry\":\"test_mix(2, test_wave)\"}]}]}")).load(
                                new ResourceLocation("test:optifine/cem/model.jem"),
                                Collections.singletonMap("head", new float[] { 0, 0, 0 }));
                assertTrue(model.warnings.toString(), model.warnings.isEmpty());
                CemModel.Instance instance = model.newInstance();
                Map<String, Double> inputs = new HashMap<>();
                inputs.put("test_wave", 0.25);
                instance.evaluate(1, inputs, pose -> {});
                int head = model.originalParts.get("head").index * CemModel.STRIDE;
                assertEquals("compiled " + compile, 0.75, instance.pose[head + 3], 1e-12);
                assertEquals("compiled " + compile, 20.25, instance.pose[head + 4], 1e-12);
            }
        } finally {
            CemConfig.compiledAnimations = compiled;
        }
    }

    @Test
    public void namesExpressionsAlreadyKnowAreRefused() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> CemApi.registerVariable("test", "time", s -> 0));
        assertThrows(
            IllegalArgumentException.class,
            () -> CemApi.registerFunction("test", "sin", CemApiTest.class.getMethod("triple", double.class)));
        assertThrows(IllegalArgumentException.class, () -> CemApi.registerVariable("test", "Bad.Name", s -> 0));
        assertThrows(
            IllegalArgumentException.class,
            () -> CemApi.registerFunction("test", "test_wrong", CemApiTest.class.getMethod("toString")));
    }
}
