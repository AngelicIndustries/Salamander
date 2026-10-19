package industries.angelic.salamander.debug.client;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.animation.state.ModelPose;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.GeoModel;

import industries.angelic.salamander.debug.entity.DebugModelEntity;

public final class DebugModel extends GeoModel<DebugModelEntity> {

    @Override
    public net.minecraft.util.ResourceLocation getModelResource(DebugModelEntity animatable) {
        return animatable.getFixture().modelResource;
    }

    @Override
    public net.minecraft.util.ResourceLocation getTextureResource(DebugModelEntity animatable) {
        return animatable.getFixture().textureResource;
    }

    @Override
    public net.minecraft.util.ResourceLocation getAnimationResource(DebugModelEntity animatable) {
        return animatable.getFixture().animationResource;
    }

    @Override
    public void setCustomAnimations(DebugModelEntity animatable, long instanceId, ModelPose pose, float partialTicks) {
        if (animatable.getFixture() != DebugModelEntity.Fixture.BAT) return;

        BoneSnapshot head = pose.get("head")
            .orElse(null);

        if (head == null) return;

        Float yaw = animatable.getAnimatableInstanceCache()
            .getManagerForId(instanceId)
            .getAnimatableData(DataTickets.NET_HEAD_YAW);
        Float pitch = animatable.getAnimatableInstanceCache()
            .getManagerForId(instanceId)
            .getAnimatableData(DataTickets.HEAD_PITCH);

        if (yaw == null || pitch == null) return;

        float degreesToRadians = (float) (Math.PI / 180D);

        head.setRotation(
            head.getRotX() - pitch * degreesToRadians,
            head.getRotY() - yaw * degreesToRadians,
            head.getRotZ());
    }
}
