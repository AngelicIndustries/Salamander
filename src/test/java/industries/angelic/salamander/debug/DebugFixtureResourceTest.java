package industries.angelic.salamander.debug;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.github.alexthe666.citadel.client.model.TabulaModel;
import com.github.alexthe666.citadel.client.model.TabulaModelHandler;
import com.github.alexthe666.citadel.client.model.container.TabulaModelContainer;

import industries.angelic.salamander.config.DebugConfig;

public class DebugFixtureResourceTest {

    private static final String[] GECKO_FIXTURES = { "creeper", "bat", "layer", "npc", "magma_spider", "jester" };

    @Test
    public void debugModeDefaultsOff() {
        assertFalse(DebugConfig.debugMode);
    }

    @Test
    public void bundledGeckoFixturesParse() throws Exception {
        GeckoLibGsonLoader loader = new GeckoLibGsonLoader();

        for (String fixture : GECKO_FIXTURES) {
            String modelPath = "/assets/salamander/geckolib/models/debug/" + fixture + ".geo.json";
            String animationPath = "/assets/salamander/geckolib/animations/debug/" + fixture + ".animation.json";

            try (InputStream stream = getClass().getResourceAsStream(modelPath)) {
                assertNotNull(modelPath, stream);

                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    assertNotNull(loader.loadModel(new ResourceLocation("salamander", "debug/" + fixture), reader));
                }
            }

            try (InputStream stream = getClass().getResourceAsStream(animationPath)) {
                assertNotNull(animationPath, stream);

                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    assertNotNull(loader.loadAnimations(reader));
                }
            }
        }
    }

    @Test
    public void bundledDragonTabulaModelConstructs() throws Exception {
        TabulaModelContainer container = TabulaModelHandler.INSTANCE
            .loadTabulaModel("/assets/salamander/models/tabula/fire_dragon");

        assertNotNull(container);
        assertFalse(
            container.getCubes()
                .isEmpty());
        assertFalse(
            new TabulaModel(container).getCubes()
                .isEmpty());
    }
}
