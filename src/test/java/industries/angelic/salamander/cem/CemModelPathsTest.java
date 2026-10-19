package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import industries.angelic.salamander.cem.loading.CemModelPaths;

public class CemModelPathsTest {

    @Test
    public void respectsPackPriorityThenEmfAndSubdirectoryPreferences() {
        Map<ResourceLocation, Integer> packs = new HashMap<>();
        ResourceLocation optifine = new ResourceLocation("optifine/cem/player.jem");
        ResourceLocation optifineSub = new ResourceLocation("optifine/cem/player/player.jem");
        ResourceLocation emf = new ResourceLocation("emf/cem/player.jem");
        ResourceLocation emfSub = new ResourceLocation("emf/cem/player/player.jem");
        assertNull(CemModelPaths.select("player", packs::containsKey, packs::get));
        packs.put(optifine, 1);
        packs.put(emf, 0);
        assertEquals(optifine, CemModelPaths.select("player", packs::containsKey, packs::get));
        packs.put(emf, 1);
        assertEquals(emf, CemModelPaths.select("player", packs::containsKey, packs::get));
        packs.put(emfSub, 1);
        assertEquals(emfSub, CemModelPaths.select("player", packs::containsKey, packs::get));
        packs.put(optifineSub, 2);
        assertEquals(optifineSub, CemModelPaths.select("player", packs::containsKey, packs::get));
    }
}
