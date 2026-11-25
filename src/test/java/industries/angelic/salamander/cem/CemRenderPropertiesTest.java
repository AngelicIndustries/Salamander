package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Collections;

import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

public class CemRenderPropertiesTest {

    private static CemModel load(String json) throws Exception {
        return new CemLoader(location -> new StringReader(json)).load(
            new ResourceLocation("test:optifine/cem/model.jem"),
            Collections.singletonMap("head", new float[] { 0, 0, 0 }));
    }

    @Test
    public void sizeAddOfAnAxisOverridesSizeAdd() throws Exception {
        CemModel model = load(
            "{\"models\":[{\"part\":\"head\",\"id\":\"cap\",\"boxes\":[{\"coordinates\":[0,0,0,1,1,1],"
                + "\"textureOffset\":[0,0],\"sizeAdd\":1,\"sizeAddY\":2}]}]}");
        assertArrayEquals(new float[] { 1, 2, 1 }, model.find("cap", null, null).boxes.get(0).inflation, 0);
    }

    @Test
    public void fireIsUnsetUntilAPackSetsItAndLeashesAndFireAreWritable() throws Exception {
        CemModel model = load(
            "{\"models\":[{\"part\":\"head\",\"animations\":[{\"render.fire_scale\":\"0.5\","
                + "\"render.fire_y\":\"1\",\"render.leash_holder_offset_y\":\"-0.25\"}]}]}");
        assertTrue(model.warnings.toString(), model.warnings.isEmpty());
        CemModel.Instance instance = model.newInstance();
        instance.evaluate(1, Collections.emptyMap(), pose -> {});
        assertEquals(0.5, instance.renderProperties.get("render.fire_scale"), 0);
        assertEquals(1, instance.renderProperties.get("render.fire_y"), 0);
        assertEquals(-0.25, instance.renderProperties.get("render.leash_holder_offset_y"), 0);
        assertTrue(Double.isNaN(instance.renderProperties.get("render.fire_height")));
        assertTrue(Double.isNaN(instance.renderProperties.get("render.fire_x")));
        assertEquals(0, instance.renderProperties.get("render.leash_holder_offset_x"), 0);
    }
}
