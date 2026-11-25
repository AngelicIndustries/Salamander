package industries.angelic.salamander.cem.client;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import industries.angelic.salamander.cem.animation.CemVariables;
import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

public class CemArmorTest {

    private static final ResourceLocation FILE = new ResourceLocation("test:optifine/cem/zombie_inner_armor.jem");

    @Test
    public void leggingsUseTheInnerModelAndTheRestTheOuter() {
        ModelBiped inner = new ModelBiped(0.5F), outer = new ModelBiped(1.0F);
        CemArmor.offer(inner, 2, inner);
        CemArmor.offer(outer, 1, outer);
        assertEquals(CemArmor.INNER, CemArmor.layer(inner));
        assertEquals(CemArmor.OUTER, CemArmor.layer(outer));
    }

    @Test
    public void anItemsOwnModelOrAnUnknownModelClassKeepsItsGeometry() {
        ModelBiped own = new ModelBiped(1.0F), custom = new ModelBiped(1.0F);
        CemArmor.offer(own, 0, custom);
        assertNull(CemArmor.layer(own));
        assertNull(CemArmor.layer(custom));
        // Animation mods subclass the biped for their own stand-ins.
        ModelBiped boned = new ModelBiped(1.0F) {};
        CemArmor.offer(boned, 0, boned);
        assertNull(CemArmor.layer(boned));
        assertTrue(
            CemTargets.armorCandidates(null, boned)
                .isEmpty());
    }

    @Test
    public void armorNamesFallBackAsEmfDoes() {
        ModelZombie leggings = new ModelZombie(0.5F, true), helmet = new ModelZombie(1.0F, true);
        CemArmor.offer(leggings, 2, leggings);
        CemArmor.offer(helmet, 0, helmet);
        EntityZombie zombie = new EntityZombie(null);
        assertEquals(Arrays.asList("zombie_inner_armor", "inner_armor"), CemTargets.armorCandidates(zombie, leggings));
        zombie.setChild(true);
        assertEquals(
            Arrays.asList("zombie_baby_outer_armor", "zombie_outer_armor", "baby_outer_armor", "outer_armor"),
            CemTargets.armorCandidates(zombie, helmet));
        assertEquals(Collections.singletonList("inner_armor"), CemTargets.armorCandidates(null, leggings));
    }

    @Test
    public void armorTakesTheWearersPoseWhereItDoesNotAnimateItself() throws Exception {
        Map<String, float[]> pivots = Collections.singletonMap("head", new float[] { 0, 0, 0 });
        CemModel armor = new CemLoader(
            location -> new StringReader(
                "{\"models\":[{\"part\":\"head\",\"id\":\"head\",\"animations\":[{\"head.rx\":\"0.5\"}]}]}"))
                    .load(FILE, pivots);
        CemModel.Instance instance = armor.newInstance(new CemVariables());
        instance.evaluate(1, Collections.emptyMap(), pose -> {});
        CemModel.Node head = armor.originalParts.get("head");
        int offset = head.index * CemModel.STRIDE;
        double[] wearer = { 1, 2, 3, 0.1, 0.2, 0.3, 1, 1.5, 1, 1, 1 };
        instance.followPose(head, wearer, 0);
        assertArrayEquals(
            new double[] { 1, 2, 3, 0.5, 0.2, 0.3, 1, 1.5, 1 },
            Arrays.copyOfRange(instance.pose, offset, offset + 9),
            0);
    }

    @Test
    public void emfAttachmentPointsLoadAndOthersAreLeftOutWithAWarning() throws Exception {
        CemModel model = new CemLoader(
            location -> new StringReader(
                "{\"models\":[{\"part\":\"head\",\"id\":\"hood\",\"attachments\":{\"head_item\":[0,1,0],"
                    + "\"witch_item\":[0,0,1],\"fox_item\":[1,0,0],\"enderman_block\":[0,2,0],\"parrot_left\":[0,0,0]}}]}"))
                        .load(FILE, Collections.singletonMap("head", new float[] { 0, 0, 0 }));
        CemModel.Node hood = model.find("hood", null, null);
        assertEquals(
            Arrays.asList("head_item", "witch_item", "fox_item", "enderman_block"),
            Arrays.asList(
                hood.attachments.keySet()
                    .toArray()));
        assertEquals(1, model.warnings.size());
        assertTrue(
            model.warnings.get(0),
            model.warnings.get(0)
                .contains("parrot_left"));
    }
}
