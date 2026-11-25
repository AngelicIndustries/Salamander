package industries.angelic.salamander.cem.client;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import industries.angelic.salamander.api.cem.CemApi;
import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

public class CemAnimationControlTest {

    private static CemModel model() throws Exception {
        Map<String, float[]> pivots = new HashMap<>();
        pivots.put("head", new float[] { 0, 0, 0 });
        pivots.put("body", new float[] { 0, 0, 0 });
        return new CemLoader(
            location -> new StringReader(
                "{\"models\":[{\"part\":\"head\",\"animations\":[{\"head.rx\":\"1\",\"body.rx\":\"2\"}]},"
                    + "{\"part\":\"body\"}]}")).load(new ResourceLocation("test:optifine/cem/model.jem"), pivots);
    }

    /** The pose a native model would set: every part turned 0.25 about x. */
    private static double[] evaluate(CemModel model, boolean paused, Set<String> pausedParts) {
        CemModel.Instance instance = model.newInstance();
        instance.evaluate(
            1,
            Collections.emptyMap(),
            pose -> {
                for (CemModel.Node node : model.nodes)
                    if (node.vanillaPart != null) pose[node.index * CemModel.STRIDE + 3] = 0.25;
            },
            paused,
            pausedParts);
        return instance.pose;
    }

    private static double rx(CemModel model, double[] pose, String part) {
        return pose[model.originalParts.get(part).index * CemModel.STRIDE + 3];
    }

    @Test
    public void pausedModelsKeepTheVanillaPoseAndPausedPartsTheirOwn() throws Exception {
        CemModel model = model();
        double[] animated = evaluate(model, false, Collections.emptySet());
        assertEquals(1, rx(model, animated, "head"), 0);
        assertEquals(2, rx(model, animated, "body"), 0);
        double[] paused = evaluate(model, true, Collections.emptySet());
        assertEquals(0.25, rx(model, paused, "head"), 0);
        assertEquals(0.25, rx(model, paused, "body"), 0);
        double[] head = evaluate(model, false, Collections.singleton("head"));
        assertEquals(0.25, rx(model, head, "head"), 0);
        assertEquals(2, rx(model, head, "body"), 0);
    }

    @Test
    public void blockEntitiesAreKnownByTheirPlace() {
        TileEntity chest = new TileEntity(), same = new TileEntity(), other = new TileEntity();
        chest.xCoord = same.xCoord = other.xCoord = 3;
        chest.yCoord = same.yCoord = 64;
        other.yCoord = 65;
        assertTrue(CemApi.pauseAnimations(chest));
        try {
            assertTrue(CemAnimationControl.paused(same));
            assertFalse(CemAnimationControl.paused(other));
            assertFalse(CemApi.pauseAnimations("not an entity"));
        } finally {
            CemApi.resumeAnimations(chest);
        }
        assertFalse(CemAnimationControl.paused(same));
        assertTrue(CemApi.pauseAnimations(chest, "lid"));
        assertEquals(Collections.singleton("lid"), CemAnimationControl.pausedParts(same));
        CemApi.resumeAnimations(chest);
        assertTrue(
            CemAnimationControl.pausedParts(same)
                .isEmpty());
        assertTrue(CemApi.lockToVanillaModel(chest));
        assertTrue(CemAnimationControl.vanilla(same));
        CemApi.unlockFromVanillaModel(chest);
        assertFalse(CemAnimationControl.vanilla(same));
    }

    @Test
    public void conditionsAndHooksDecideAsEmfs() {
        TileEntity marked = new TileEntity(), plain = new TileEntity();
        marked.xCoord = 1000;
        CemApi
            .registerPauseCondition(subject -> subject instanceof TileEntity && ((TileEntity) subject).xCoord == 1000);
        CemApi.registerVanillaModelCondition(
            subject -> { throw new IllegalStateException("a failing condition does not hold"); });
        assertTrue(CemAnimationControl.paused(marked));
        assertFalse(CemAnimationControl.paused(plain));
        assertFalse(CemAnimationControl.vanilla(plain));
        int[] ends = new int[1];
        CemApi.registerAnimationHook(new CemApi.AnimationHook() {

            @Override
            public boolean onAnimationStart(Object subject, String model, Set<String> pausedParts, boolean cancelled) {
                return subject != marked;
            }

            @Override
            public void onAnimationEnd(Object subject, String model, Set<String> pausedParts, boolean cancelled) {
                ends[0]++;
            }
        });
        CemApi.registerAnimationHook(new CemApi.AnimationHook() {

            @Override
            public boolean onAnimationStart(Object subject, String model, Set<String> pausedParts, boolean cancelled) {
                // Later hooks still hear of a run an earlier one cancelled.
                assertEquals(subject == marked, cancelled);
                return true;
            }
        });
        assertTrue(CemAnimationControl.start(marked, "test:model.jem", Collections.emptySet()));
        assertFalse(CemAnimationControl.start(plain, "test:model.jem", Collections.emptySet()));
        CemAnimationControl.end(plain, "test:model.jem", Collections.emptySet(), false);
        assertEquals(1, ends[0]);
    }
}
