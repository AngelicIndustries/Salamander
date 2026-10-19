/*
 * Derived from Citadel, Copyright (c) AlexModGuy and contributors.
 * SPDX-License-Identifier: LGPL-3.0-only
 * Modified for Minecraft 1.7.10 by the Salamander contributors.
 */
package com.github.alexthe666.citadel.client.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.container.Transform;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.mixins.early.minecraft.client.AccessorMinecraft;

/** Citadel's imperative keyframe animator. */
@SideOnly(Side.CLIENT)
public class ModelAnimator {

    private static DoubleSupplier partialTickSupplier = ModelAnimator::getClientPartialTick;

    private int tempTick;
    private int previousTempTick;
    private boolean correctAnimation;
    private IAnimatedEntity entity;
    private final Map<AdvancedModelBox, Transform> transformMap = new HashMap<>();
    private final Map<AdvancedModelBox, Transform> previousTransformMap = new HashMap<>();

    public ModelAnimator() {}

    public static ModelAnimator create() {
        return new ModelAnimator();
    }

    public IAnimatedEntity getEntity() {
        return this.entity;
    }

    public void update(IAnimatedEntity entity) {
        this.tempTick = 0;
        this.previousTempTick = 0;
        this.correctAnimation = false;
        this.entity = entity;
        this.transformMap.clear();
        this.previousTransformMap.clear();
    }

    public boolean setAnimation(Animation animation) {
        this.tempTick = 0;
        this.previousTempTick = 0;
        this.correctAnimation = this.entity != null && this.entity.getAnimation() == animation;
        return this.correctAnimation;
    }

    public void startKeyframe(int duration) {
        if (!this.correctAnimation) return;
        this.previousTempTick = this.tempTick;
        this.tempTick += duration;
    }

    public void setStaticKeyframe(int duration) {
        startKeyframe(duration);
        endKeyframe(true);
    }

    public void resetKeyframe(int duration) {
        startKeyframe(duration);
        endKeyframe();
    }

    public void rotate(AdvancedModelBox box, float x, float y, float z) {
        if (this.correctAnimation) getTransform(box).addRotation(x, y, z);
    }

    public void move(AdvancedModelBox box, float x, float y, float z) {
        if (this.correctAnimation) getTransform(box).addOffset(x, y, z);
    }

    public void endKeyframe() {
        endKeyframe(false);
    }

    static void setPartialTickSupplierForTests(DoubleSupplier supplier) {
        partialTickSupplier = supplier == null ? ModelAnimator::getClientPartialTick : supplier;
    }

    private Transform getTransform(AdvancedModelBox box) {
        Transform transform = this.transformMap.get(box);

        if (transform == null) {
            transform = new Transform();
            this.transformMap.put(box, transform);
        }

        return transform;
    }

    private void endKeyframe(boolean stationary) {
        if (!this.correctAnimation) return;

        int animationTick = this.entity.getAnimationTick();

        if (animationTick >= this.previousTempTick && animationTick < this.tempTick) {
            if (stationary) applyTransforms(this.previousTransformMap, 1);
            else {
                float tick = (animationTick - this.previousTempTick + (float) partialTickSupplier.getAsDouble())
                    / (this.tempTick - this.previousTempTick);
                float increasingWeight = MathHelper.sin((float) (tick * Math.PI / 2));
                applyTransforms(this.previousTransformMap, 1 - increasingWeight);
                applyTransforms(this.transformMap, increasingWeight);
            }
        }

        if (!stationary) {
            this.previousTransformMap.clear();
            this.previousTransformMap.putAll(this.transformMap);
            this.transformMap.clear();
        }
    }

    private static void applyTransforms(Map<AdvancedModelBox, Transform> transforms, float weight) {
        for (Map.Entry<AdvancedModelBox, Transform> entry : transforms.entrySet()) {
            AdvancedModelBox box = entry.getKey();
            Transform transform = entry.getValue();
            box.rotateAngleX += weight * transform.getRotationX();
            box.rotateAngleY += weight * transform.getRotationY();
            box.rotateAngleZ += weight * transform.getRotationZ();
            box.rotationPointX += weight * transform.getOffsetX();
            box.rotationPointY += weight * transform.getOffsetY();
            box.rotationPointZ += weight * transform.getOffsetZ();
        }
    }

    private static double getClientPartialTick() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.isGamePaused() ? 0 : ((AccessorMinecraft) minecraft).salamander$getTimer().renderPartialTicks;
    }
}
