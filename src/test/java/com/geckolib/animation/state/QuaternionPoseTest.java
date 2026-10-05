package com.geckolib.animation.state;

import static org.junit.Assert.*;

import java.util.Collections;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.Test;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;

public class QuaternionPoseTest {

    private static final float EPSILON = 1e-6f;

    @Test
    public void normalizesDefensivelyAndRejectsInvalidInputsWithoutMutation() {
        BoneSnapshot pose = BoneSnapshot.create("root");
        assertFalse(pose.hasAdditionalRotation());
        Quaternionf input = new Quaternionf(0, 0, 2, 2);
        pose.setAdditionalRotation(input);
        input.identity();
        Quaternionf copy = pose.getAdditionalRotation(new Quaternionf());
        assertEquals(1, copy.lengthSquared(), EPSILON);
        assertTrue(pose.hasAdditionalRotation());
        copy.identity();
        assertTrue(pose.hasAdditionalRotation());
        Quaternionf before = pose.getAdditionalRotation(new Quaternionf());
        float[][] invalid = { { 0, 0, 0, 0 }, { Float.NaN, 0, 0, 1 }, { 0, Float.POSITIVE_INFINITY, 0, 1 } };
        for (float[] q : invalid) {
            assertThrows(IllegalArgumentException.class, () -> pose.setAdditionalRotation(q[0], q[1], q[2], q[3]));
            assertEquals(before, pose.getAdditionalRotation(new Quaternionf()));
        }
        pose.setAdditionalRotation(Float.MAX_VALUE, 0, 0, Float.MAX_VALUE);
        assertEquals(
            1,
            pose.getAdditionalRotation(copy)
                .lengthSquared(),
            EPSILON);
        pose.setAdditionalRotation(Float.MIN_VALUE, 0, 0, Float.MIN_VALUE);
        assertEquals(
            1,
            pose.getAdditionalRotation(copy)
                .lengthSquared(),
            EPSILON);
        pose.resetAdditionalRotation();
        assertFalse(pose.hasAdditionalRotation());
    }

    @Test
    public void composesInDeclaredOrderAndIsolatesCopiesAndModelInstances() {
        GeoBone root = new CuboidGeoBone(
            null,
            "root",
            new GeoBone[0],
            new GeoCube[0],
            new GeoLocator[0],
            0,
            0,
            0,
            0,
            0,
            0);
        BakedGeoModel model = new BakedGeoModel(new GeoBone[] { root }, Collections.emptyMap(), null);
        ModelPose first = ModelPose.create(model);
        ModelPose second = ModelPose.create(model);
        BoneSnapshot z = BoneSnapshot.create("root")
            .setAdditionalRotation(new Quaternionf().rotationZ((float) Math.PI / 2));
        BoneSnapshot x = BoneSnapshot.create("root")
            .setAdditionalRotation(new Quaternionf().rotationX((float) Math.PI / 2));
        first.apply(Collections.singletonMap("root", z), false);
        first.apply(Collections.singletonMap("root", x), true);
        Vector3f point = first.get(root)
            .getAdditionalRotation(new Quaternionf())
            .transform(new Vector3f(0, 1, 0));
        // X maps +Y to +Z; Z leaves +Z unchanged. Reversed composition maps +Y to -X.
        assertEquals(0, point.x, EPSILON);
        assertEquals(0, point.y, EPSILON);
        assertEquals(1, point.z, EPSILON);
        assertFalse(
            second.get(root)
                .hasAdditionalRotation());
        BoneSnapshot copy = first.get(root)
            .copy();
        first.get(root)
            .resetAdditionalRotation();
        assertTrue(copy.hasAdditionalRotation());
        assertTrue(z.hasAdditionalRotation());
        second.apply(Collections.singletonMap("root", copy), false);
        copy.resetAdditionalRotation();
        assertTrue(
            second.get(root)
                .hasAdditionalRotation());
        second.apply(Collections.singletonMap("root", BoneSnapshot.create("root")), false);
        assertFalse(
            second.get(root)
                .hasAdditionalRotation());
    }
}
