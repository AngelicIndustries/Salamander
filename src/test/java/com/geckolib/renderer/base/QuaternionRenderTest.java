package com.geckolib.renderer.base;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.Test;

import com.geckolib.animation.state.ModelPose;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVector;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;

public class QuaternionRenderTest {

    private static final float EPSILON = 1e-5f;

    @Test
    public void preservesBindRotationPivotsScaleNormalsAndAttachmentMatrices() {
        BakedGeoModel model = model();
        ModelPose pose = ModelPose.create(model);
        pose.get("root")
            .get()
            .setTranslation(2, 3, 4)
            .setRotation(.1f, .2f, .3f)
            .setAdditionalRotation(new Quaternionf().rotationXYZ(.4f, -.2f, .6f))
            .setScale(2, 3, .5f);
        pose.get("child")
            .get()
            .setTranslation(-1, 2, 0)
            .setAdditionalRotation(new Quaternionf().rotationY(-.7f));
        Matrix4f root = new Matrix4f().translate(-2 / 16f, 3 / 16f, 4 / 16f)
            .translate(.5f, .25f, -.125f)
            .rotateZ(.6f)
            .rotateY(.4f)
            .rotateX(.2f)
            .rotateXYZ(.4f, -.2f, .6f)
            .scale(2, 3, .5f)
            .translate(-.5f, -.25f, .125f);
        Matrix4f pivot = new Matrix4f(root).translate(1 / 16f, 2 / 16f, 0)
            .translate(1, 0, 0)
            .rotateZ(-.2f)
            .rotateY(.1f)
            .rotateX(.3f)
            .rotateY(-.7f);
        Matrix4f geometry = new Matrix4f(pivot).translate(-1, 0, 0);
        GeoRenderTransforms transforms = GeoModelRenderer
            .captureTransforms(model, pose, new Matrix4f().translation(10, 20, 30));
        assertMatrix(
            pivot,
            transforms.getBone("child")
                .get()
                .modelSpaceMatrix());
        assertMatrix(
            geometry,
            transforms.getBone("child")
                .get()
                .geometryMatrix());
        Matrix4f locator = new Matrix4f(geometry).translate(1, .5f, .25f)
            .rotateZ(.2f);
        assertMatrix(
            locator,
            transforms.getLocator("anchor")
                .get()
                .modelSpaceMatrix());
        assertMatrix(
            new Matrix4f().translation(10, 20, 30)
                .mul(locator),
            transforms.getLocator("anchor")
                .get()
                .renderSpaceMatrix());
        List<float[]> vertices = render(model, pose);
        assertEquals(4, vertices.size());
        Vector3f normal = new Matrix3f(geometry).normal()
            .transform(new Vector3f(0, 0, 1))
            .normalize();
        for (int i = 0; i < 4; i++) {
            float[] v = vertices.get(i);
            Vector3f position = geometry.transformPosition(new Vector3f(i % 2, i / 2, 0));
            assertVector(position, v, 0);
            assertVector(normal, v, 5);
            assertEquals(i / 4f, v[3], EPSILON);
            assertEquals(1 - i / 4f, v[4], EPSILON);
        }
        List<float[]> untouched = render(model, ModelPose.create(model));
        assertFalse(java.util.Arrays.equals(vertices.get(0), untouched.get(0)));
        pose.get("child")
            .get()
            .skipRender(true);
        assertTrue(render(model, pose).isEmpty());
        // Hidden geometry still provides functional attachment transforms.
        assertMatrix(
            locator,
            GeoModelRenderer.captureTransforms(model, pose)
                .getLocator("anchor")
                .get()
                .modelSpaceMatrix());
        pose.get("child")
            .get()
            .skipRender(false);
        pose.get("root")
            .get()
            .skipChildrenRender(true);
        assertTrue(render(model, pose).isEmpty());
    }

    @Test
    public void identityIsExactlyTheExistingEulerPath() {
        BakedGeoModel model = model();
        ModelPose pose = ModelPose.create(model);
        pose.get("root")
            .get()
            .setRotation(.2f, .3f, .4f);
        List<float[]> before = render(model, pose);
        pose.get("root")
            .get()
            .setAdditionalRotation(0, 0, 0, -2);
        List<float[]> after = render(model, pose);
        for (int i = 0; i < before.size(); i++) assertArrayEquals(before.get(i), after.get(i), 0);
    }

    private static BakedGeoModel model() {
        GeoVertex[] vertices = new GeoVertex[4];
        for (int i = 0; i < 4; i++) vertices[i] = new GeoVertex(i % 2, i / 2, 0, i / 4f, 1 - i / 4f);
        GeoCube cube = new GeoCube(
            new GeoQuad[] { new GeoQuad(vertices, 0, 0, 1, ForgeDirection.SOUTH) },
            GeoVector.ZERO,
            GeoVector.ZERO,
            new GeoVector(1, 1, 1));
        GeoBone[] children = new GeoBone[1];
        GeoBone root = new CuboidGeoBone(
            null,
            "root",
            children,
            new GeoCube[0],
            new GeoLocator[0],
            8,
            4,
            -2,
            .1f,
            .2f,
            .3f);
        GeoLocator[] anchors = new GeoLocator[1];
        GeoBone child = new CuboidGeoBone(
            root,
            "child",
            new GeoBone[0],
            new GeoCube[] { cube },
            anchors,
            16,
            0,
            0,
            .3f,
            .1f,
            -.2f);
        children[0] = child;
        anchors[0] = new GeoLocator(child, "anchor", 16, 8, 4, 0, 0, .2f);
        return new BakedGeoModel(new GeoBone[] { root }, Collections.singletonMap("anchor", anchors[0]), null);
    }

    private static List<float[]> render(BakedGeoModel model, ModelPose pose) {
        List<float[]> vertices = new ArrayList<>();
        GeoModelRenderer.render(
            model,
            pose,
            (x, y, z, u, v, nx, ny, nz, r, g, b, a) -> vertices.add(new float[] { x, y, z, u, v, nx, ny, nz }),
            1,
            1,
            1,
            1);
        return vertices;
    }

    private static void assertVector(Vector3f expected, float[] actual, int offset) {
        assertEquals(expected.x, actual[offset], EPSILON);
        assertEquals(expected.y, actual[offset + 1], EPSILON);
        assertEquals(expected.z, actual[offset + 2], EPSILON);
    }

    private static void assertMatrix(Matrix4f expected, Matrix4f actual) {
        assertArrayEquals(expected.get(new float[16]), actual.get(new float[16]), EPSILON);
    }
}
