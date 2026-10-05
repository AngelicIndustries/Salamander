package com.geckolib.animation.state;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;

/** Per-render transform state for an immutable baked model. */
public final class ModelPose {

    private final BakedGeoModel model;
    private final Map<String, BoneSnapshot> bones;

    private ModelPose(BakedGeoModel model, Map<String, BoneSnapshot> bones) {
        this.model = model;
        this.bones = bones;
    }

    public static ModelPose create(BakedGeoModel model) {
        Map<String, BoneSnapshot> bones = new LinkedHashMap<>();

        for (GeoBone bone : model.boneLookup()
            .values()) {
            bones.put(bone.name(), BoneSnapshot.create(bone));
        }

        return new ModelPose(model, bones);
    }

    public BakedGeoModel model() {
        return this.model;
    }

    public Optional<BoneSnapshot> get(String boneName) {
        return Optional.ofNullable(this.bones.get(boneName));
    }

    public BoneSnapshot get(GeoBone bone) {
        BoneSnapshot snapshot = this.bones.get(bone.name());

        if (snapshot == null) throw new IllegalArgumentException("Bone does not belong to this model: " + bone.name());

        return snapshot;
    }

    public Map<String, BoneSnapshot> bones() {
        return Collections.unmodifiableMap(this.bones);
    }

    public void apply(Map<String, BoneSnapshot> controllerPose, boolean additive) {
        for (Map.Entry<String, BoneSnapshot> entry : controllerPose.entrySet()) {
            BoneSnapshot target = this.bones.get(entry.getKey());

            if (target == null) continue;

            BoneSnapshot source = entry.getValue();
            target.applyAdditionalRotation(source, additive);

            if (additive) {
                target.setScale(
                    target.getScaleX() * source.getScaleX(),
                    target.getScaleY() * source.getScaleY(),
                    target.getScaleZ() * source.getScaleZ());
                target.setRotation(
                    target.getRotX() + source.getRotX(),
                    target.getRotY() + source.getRotY(),
                    target.getRotZ() + source.getRotZ());
                target.setTranslation(
                    target.getTranslateX() + source.getTranslateX(),
                    target.getTranslateY() + source.getTranslateY(),
                    target.getTranslateZ() + source.getTranslateZ());
                target.skipRender(target.isHidden() || source.isHidden());
                target.skipChildrenRender(target.areChildrenHidden() || source.areChildrenHidden());
            } else {
                target.setScale(source.getScaleX(), source.getScaleY(), source.getScaleZ());
                target.setRotation(source.getRotX(), source.getRotY(), source.getRotZ());
                target.setTranslation(source.getTranslateX(), source.getTranslateY(), source.getTranslateZ());
                target.skipRender(source.isHidden());
                target.skipChildrenRender(source.areChildrenHidden());
            }
        }
    }
}
