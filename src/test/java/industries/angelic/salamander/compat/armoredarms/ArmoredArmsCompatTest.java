package industries.angelic.salamander.compat.armoredarms;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
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

import industries.angelic.salamander.cem.client.CemBinding;
import industries.angelic.salamander.cem.client.CemPlayers;
import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

public class ArmoredArmsCompatTest {

    @Test
    public void worldPoseAndVisibilityDoNotReachTheHandOrArmorBones() throws Exception {
        ModelBiped world = new ModelBiped();
        world.bipedBody.addChild(world.bipedRightArm);
        world.bipedRightArm.rotationPointY = -10;
        world.bipedRightArm.rotateAngleX = 1;
        world.bipedRightArm.showModel = false;
        world.bipedRightArm.isHidden = true;
        Manager manager = new Manager(world);
        ModelBiped hand = ArmoredArmsCompat
            .prepareHand(manager, Manager.class.getMethod("prepareModel", ModelBiped.class));
        assertNotSame(world, hand);
        assertTrue(CemPlayers.registered(hand));
        assertEquals(2, hand.bipedRightArm.rotationPointY, 0);
        assertTrue(hand.bipedRightArm.showModel);
        assertFalse(hand.bipedRightArm.isHidden);
        assertEquals(-10, world.bipedRightArm.rotationPointY, 0);
        assertEquals(1, world.bipedRightArm.rotateAngleX, 0);
        assertFalse(world.bipedRightArm.showModel);
        assertTrue(world.bipedRightArm.isHidden);
        assertEquals(2, manager.rightBone[1], 0);
    }

    @Test
    public void faPlayerStaticHandAndSleeveUseTheArmorBoneForBothSkinWidths() throws Exception {
        String directory = System.getenv("SALAMANDER_CEM_PACK_DIR");
        Assume.assumeNotNull(directory);
        File file = new File(directory, "FA+Player-v1.1.zip");
        Assume.assumeTrue(file.isFile());
        Manager manager = new Manager(new ModelBiped());
        ModelBiped hand = ArmoredArmsCompat
            .prepareHand(manager, Manager.class.getMethod("prepareModel", ModelBiped.class));
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", hand.bipedHead);
        parts.put("headwear", hand.bipedHeadwear);
        parts.put("body", hand.bipedBody);
        parts.put("right_arm", hand.bipedRightArm);
        parts.put("left_arm", hand.bipedLeftArm);
        parts.put("right_leg", hand.bipedRightLeg);
        parts.put("left_leg", hand.bipedLeftLeg);
        parts.put("cloak", hand.bipedCloak);
        CemBinding binding = new CemBinding(hand, parts);
        try (ZipFile zip = new ZipFile(file)) {
            for (String target : new String[] { "player", "player_slim" }) {
                CemModel model = new CemLoader(location -> {
                    ZipEntry entry = zip
                        .getEntry("assets/" + location.getResourceDomain() + "/" + location.getResourcePath());
                    if (entry == null) throw new FileNotFoundException(location.toString());
                    return new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8);
                }).load(new ResourceLocation("emf/cem/" + target + ".jem"), binding.pivots, binding.parents, 64, 64);
                CemModel.Instance instance = model.newInstance(new HashMap<>());
                instance.staticPose(pose -> binding.pose(model, pose, null, parts));
                binding.playerLayers(instance, true);
                for (String name : new String[] { "right_arm", "right_sleeve" }) {
                    CemModel.Node node = model.originalParts.get(name);
                    binding.refresh(node, instance, null, parts);
                    int offset = node.index * CemModel.STRIDE;
                    for (int axis = 0; axis < 6; axis++)
                        assertEquals(target + " " + name, manager.rightBone[axis], instance.pose[offset + axis], 0);
                    assertEquals(1, instance.pose[offset + 9], 0);
                    assertEquals(1, instance.pose[offset + 10], 0);
                }
                // FA's custom arm box must retain vanilla arm-local placement under that shared bone.
                CemModel.Node arm = model.originalParts.get("right_arm").children.get(0);
                CemModel.Box box = arm.boxes.get(0);
                assertEquals(1, instance.pose[arm.index * CemModel.STRIDE + 9], 0);
                assertEquals(1, instance.pose[arm.index * CemModel.STRIDE + 10], 0);
                double width = target.equals("player_slim") ? 3 : 4;
                double[] bounds = new double[6];
                for (int axis = 0; axis < 3; axis++) {
                    bounds[axis] = arm.transform[axis] + box.coordinates[axis];
                    bounds[axis + 3] = box.coordinates[axis + 3];
                }
                assertArrayEquals(new double[] { 1 - width, -2, -2, width, 12, 4 }, bounds, 0);
            }
        }
    }

    /** The prepareModel contract from ArmoredArms 1.3.9; no optional mod classes are needed by this test. */
    public static final class Manager {

        public final ModelBiped world;
        public float[] rightBone;

        Manager(ModelBiped world) {
            this.world = world;
        }

        public void prepareModel(ModelBiped model) {
            model.onGround = 0;
            model.isRiding = false;
            model.setRotationAngles(0, 0, 0, 0, 0, .0625F, null);
            ModelRenderer arm = model.bipedRightArm;
            rightBone = new float[] { arm.rotationPointX, arm.rotationPointY, arm.rotationPointZ, arm.rotateAngleX,
                arm.rotateAngleY, arm.rotateAngleZ };
        }
    }
}
