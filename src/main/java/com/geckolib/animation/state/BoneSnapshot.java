package com.geckolib.animation.state;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;

import com.geckolib.cache.model.GeoBone;

/** CPU-side pose values for one named bone. */
public final class BoneSnapshot {

    private final String boneName;
    private final GeoBone bone;
    private float scaleX = 1;
    private float scaleY = 1;
    private float scaleZ = 1;
    private float translateX;
    private float translateY;
    private float translateZ;
    private float rotX;
    private float rotY;
    private float rotZ;
    private final Quaternionf additionalRotation = new Quaternionf();
    private boolean hidden;
    private boolean childrenHidden;

    private BoneSnapshot(String boneName, GeoBone bone) {
        this.boneName = boneName;
        this.bone = bone;
    }

    public static BoneSnapshot create(String boneName) {
        return new BoneSnapshot(boneName, null);
    }

    public static BoneSnapshot create(GeoBone bone) {
        return new BoneSnapshot(bone.name(), bone);
    }

    public String getBoneName() {
        return this.boneName;
    }

    public GeoBone getBone() {
        return this.bone;
    }

    public float getScaleX() {
        return this.scaleX;
    }

    public float getScaleY() {
        return this.scaleY;
    }

    public float getScaleZ() {
        return this.scaleZ;
    }

    public float getTranslateX() {
        return this.translateX;
    }

    public float getTranslateY() {
        return this.translateY;
    }

    public float getTranslateZ() {
        return this.translateZ;
    }

    public float getRotX() {
        return this.rotX;
    }

    public float getRotY() {
        return this.rotY;
    }

    public float getRotZ() {
        return this.rotZ;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public boolean areChildrenHidden() {
        return this.childrenHidden;
    }

    public BoneSnapshot setScale(float x, float y, float z) {
        this.scaleX = x;
        this.scaleY = y;
        this.scaleZ = z;

        return this;
    }

    public BoneSnapshot setScaleX(float value) {
        this.scaleX = value;

        return this;
    }

    public BoneSnapshot setScaleY(float value) {
        this.scaleY = value;

        return this;
    }

    public BoneSnapshot setScaleZ(float value) {
        this.scaleZ = value;

        return this;
    }

    public BoneSnapshot setTranslation(float x, float y, float z) {
        this.translateX = x;
        this.translateY = y;
        this.translateZ = z;

        return this;
    }

    public BoneSnapshot setTranslateX(float value) {
        this.translateX = value;

        return this;
    }

    public BoneSnapshot setTranslateY(float value) {
        this.translateY = value;

        return this;
    }

    public BoneSnapshot setTranslateZ(float value) {
        this.translateZ = value;

        return this;
    }

    public BoneSnapshot setRotation(float x, float y, float z) {
        this.rotX = x;
        this.rotY = y;
        this.rotZ = z;

        return this;
    }

    public BoneSnapshot setRotX(float value) {
        this.rotX = value;

        return this;
    }

    public BoneSnapshot setRotY(float value) {
        this.rotY = value;

        return this;
    }

    public BoneSnapshot setRotZ(float value) {
        this.rotZ = value;

        return this;
    }

    /** Copies the additional local rotation; callers never receive the mutable pose value. */
    public Quaternionf getAdditionalRotation(Quaternionf destination) {
        return destination.set(this.additionalRotation);
    }

    /**
     * Sets a normalized rotation applied after bind-plus-pose Euler rotation and before scale.
     * Components use renderer coordinates, without further axis conversion. Invalid input leaves the pose unchanged.
     */
    public BoneSnapshot setAdditionalRotation(float x, float y, float z, float w) {
        double lengthSquared = (double) x * x + (double) y * y + (double) z * z + (double) w * w;
        if (!Double.isFinite(lengthSquared) || lengthSquared == 0) {
            throw new IllegalArgumentException("Additional rotation must be finite and nonzero");
        }
        double inverseLength = 1 / Math.sqrt(lengthSquared);
        this.additionalRotation.set(
            (float) (x * inverseLength),
            (float) (y * inverseLength),
            (float) (z * inverseLength),
            (float) (w * inverseLength));
        return this;
    }

    public BoneSnapshot setAdditionalRotation(Quaternionfc rotation) {
        return setAdditionalRotation(rotation.x(), rotation.y(), rotation.z(), rotation.w());
    }

    public BoneSnapshot resetAdditionalRotation() {
        this.additionalRotation.identity();
        return this;
    }

    public boolean hasAdditionalRotation() {
        return this.additionalRotation.x != 0 || this.additionalRotation.y != 0 || this.additionalRotation.z != 0;
    }

    /** Additive order is target * source, so source acts first on column vectors. */
    void applyAdditionalRotation(BoneSnapshot source, boolean additive) {
        if (additive) this.additionalRotation.mul(source.additionalRotation)
            .normalize();
        else this.additionalRotation.set(source.additionalRotation);
    }

    public BoneSnapshot skipRender(boolean shouldSkip) {
        this.hidden = shouldSkip;

        return this;
    }

    public BoneSnapshot skipChildrenRender(boolean shouldSkip) {
        this.childrenHidden = shouldSkip;

        return this;
    }

    public BoneSnapshot copy() {
        return new BoneSnapshot(this.boneName, this.bone).setScale(this.scaleX, this.scaleY, this.scaleZ)
            .setRotation(this.rotX, this.rotY, this.rotZ)
            .setAdditionalRotation(this.additionalRotation)
            .setTranslation(this.translateX, this.translateY, this.translateZ)
            .skipRender(this.hidden)
            .skipChildrenRender(this.childrenHidden);
    }
}
