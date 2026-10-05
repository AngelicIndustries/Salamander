package com.geckolib.renderer.base;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.animation.state.ModelPose;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVector;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;

/** CPU-side hierarchical model transformation and vertex emission. */
public final class GeoModelRenderer {

    private static final float MODEL_SCALE = 1 / 16f;
    private static final float NORMAL_EPSILON = 1.0E-8f;

    private GeoModelRenderer() {}

    public static void render(BakedGeoModel model, ModelPose pose, GeoVertexConsumer consumer, float red, float green,
        float blue, float alpha) {
        render(model, pose, bone -> true, consumer, red, green, blue, alpha);
    }

    /** Emits only geometry belonging to bones accepted by {@code boneFilter}. */
    public static void render(BakedGeoModel model, ModelPose pose, Predicate<GeoBone> boneFilter,
        GeoVertexConsumer consumer, float red, float green, float blue, float alpha) {
        if (pose.model() != model) throw new IllegalArgumentException("Model pose belongs to a different baked model");

        Matrix4f identity = new Matrix4f();

        for (GeoBone bone : model.topLevelBones()) {
            renderBone(bone, pose, identity, boneFilter, consumer, red, green, blue, alpha);
        }
    }

    public static GeoRenderTransforms captureTransforms(BakedGeoModel model, ModelPose pose) {
        return captureTransforms(model, pose, new Matrix4f());
    }

    /** Captures animated bone and locator matrices without mutating the baked model or pose. */
    public static GeoRenderTransforms captureTransforms(BakedGeoModel model, ModelPose pose,
        Matrix4f renderRootMatrix) {
        if (pose.model() != model) throw new IllegalArgumentException("Model pose belongs to a different baked model");

        Map<String, GeoRenderTransform> bones = new LinkedHashMap<>();
        Map<String, GeoRenderTransform> locators = new LinkedHashMap<>();

        for (GeoBone bone : model.topLevelBones()) {
            captureBoneTransforms(bone, pose, new Matrix4f(), renderRootMatrix, bones, locators);
        }

        return new GeoRenderTransforms(bones, locators);
    }

    private static void renderBone(GeoBone bone, ModelPose pose, Matrix4f parentMatrix, Predicate<GeoBone> boneFilter,
        GeoVertexConsumer consumer, float red, float green, float blue, float alpha) {
        BoneSnapshot snapshot = pose.get(bone);
        Matrix4f boneMatrix = new Matrix4f(parentMatrix);

        transformBone(boneMatrix, bone, snapshot);

        if (boneFilter.test(bone) && !snapshot.isHidden() && bone instanceof CuboidGeoBone) {
            for (GeoCube cube : ((CuboidGeoBone) bone).cubes()) {
                renderCube(cube, boneMatrix, consumer, red, green, blue, alpha);
            }
        }

        if (!snapshot.areChildrenHidden()) {
            for (GeoBone child : bone.children()) {
                renderBone(child, pose, boneMatrix, boneFilter, consumer, red, green, blue, alpha);
            }
        }
    }

    private static void captureBoneTransforms(GeoBone bone, ModelPose pose, Matrix4f parentMatrix,
        Matrix4f renderRootMatrix, Map<String, GeoRenderTransform> bones, Map<String, GeoRenderTransform> locators) {
        BoneSnapshot snapshot = pose.get(bone);
        Matrix4f pivotMatrix = new Matrix4f(parentMatrix);

        transformToBonePivot(pivotMatrix, bone, snapshot);

        Matrix4f geometryMatrix = new Matrix4f(pivotMatrix)
            .translate(-bone.pivotX() * MODEL_SCALE, -bone.pivotY() * MODEL_SCALE, -bone.pivotZ() * MODEL_SCALE);
        bones.put(
            bone.name(),
            new GeoRenderTransform(pivotMatrix, geometryMatrix, new Matrix4f(renderRootMatrix).mul(pivotMatrix)));

        for (GeoLocator locator : bone.locators()) {
            Matrix4f locatorMatrix = new Matrix4f(geometryMatrix)
                .translate(
                    locator.offsetX() * MODEL_SCALE,
                    locator.offsetY() * MODEL_SCALE,
                    locator.offsetZ() * MODEL_SCALE)
                .rotateZ(locator.rotZ())
                .rotateY(locator.rotY())
                .rotateX(locator.rotX());

            locators.put(
                locator.name(),
                new GeoRenderTransform(
                    locatorMatrix,
                    locatorMatrix,
                    new Matrix4f(renderRootMatrix).mul(locatorMatrix)));
        }

        for (GeoBone child : bone.children()) {
            captureBoneTransforms(child, pose, geometryMatrix, renderRootMatrix, bones, locators);
        }
    }

    private static void transformBone(Matrix4f matrix, GeoBone bone, BoneSnapshot snapshot) {
        transformToBonePivot(matrix, bone, snapshot);
        matrix.translate(-bone.pivotX() * MODEL_SCALE, -bone.pivotY() * MODEL_SCALE, -bone.pivotZ() * MODEL_SCALE);
    }

    private static void transformToBonePivot(Matrix4f matrix, GeoBone bone, BoneSnapshot snapshot) {
        matrix.translate(
            -snapshot.getTranslateX() * MODEL_SCALE,
            snapshot.getTranslateY() * MODEL_SCALE,
            snapshot.getTranslateZ() * MODEL_SCALE);
        matrix.translate(bone.pivotX() * MODEL_SCALE, bone.pivotY() * MODEL_SCALE, bone.pivotZ() * MODEL_SCALE);
        matrix.rotateZ(bone.baseRotZ() + snapshot.getRotZ());
        matrix.rotateY(bone.baseRotY() + snapshot.getRotY());
        matrix.rotateX(bone.baseRotX() + snapshot.getRotX());
        if (snapshot.hasAdditionalRotation()) matrix.rotate(snapshot.getAdditionalRotation(new Quaternionf()));
        matrix.scale(snapshot.getScaleX(), snapshot.getScaleY(), snapshot.getScaleZ());
    }

    private static void renderCube(GeoCube cube, Matrix4f boneMatrix, GeoVertexConsumer consumer, float red,
        float green, float blue, float alpha) {
        Matrix4f cubeMatrix = new Matrix4f(boneMatrix);
        GeoVector pivot = cube.pivot();
        GeoVector rotation = cube.rotation();

        cubeMatrix.translate(
            (float) pivot.x() * MODEL_SCALE,
            (float) pivot.y() * MODEL_SCALE,
            (float) pivot.z() * MODEL_SCALE);
        cubeMatrix.rotateZ((float) rotation.z());
        cubeMatrix.rotateY((float) rotation.y());
        cubeMatrix.rotateX((float) rotation.x());
        cubeMatrix.translate(
            (float) -pivot.x() * MODEL_SCALE,
            (float) -pivot.y() * MODEL_SCALE,
            (float) -pivot.z() * MODEL_SCALE);

        Matrix3f normalMatrix = new Matrix3f().set(cubeMatrix);

        if (Math.abs(normalMatrix.determinant()) > NORMAL_EPSILON) normalMatrix.normal();
        else normalMatrix.identity();

        Vector3f normal = new Vector3f();
        Vector4f position = new Vector4f();

        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;

            normal.set(quad.normalX(), quad.normalY(), quad.normalZ());
            normalMatrix.transform(normal);
            fixFlatCubeNormal(cube, normal);

            if (normal.lengthSquared() > NORMAL_EPSILON) normal.normalize();

            for (GeoVertex vertex : quad.vertices()) {
                position.set(vertex.posX(), vertex.posY(), vertex.posZ(), 1);
                cubeMatrix.transform(position);
                consumer.addVertex(
                    position.x,
                    position.y,
                    position.z,
                    vertex.texU(),
                    vertex.texV(),
                    normal.x,
                    normal.y,
                    normal.z,
                    red,
                    green,
                    blue,
                    alpha);
            }
        }
    }

    private static void fixFlatCubeNormal(GeoCube cube, Vector3f normal) {
        GeoVector size = cube.size();

        if (normal.x < 0 && (size.y() == 0 || size.z() == 0)) normal.x *= -1;

        if (normal.y < 0 && (size.x() == 0 || size.z() == 0)) normal.y *= -1;

        if (normal.z < 0 && (size.x() == 0 || size.y() == 0)) normal.z *= -1;
    }
}
