package industries.angelic.salamander.debug.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;

/** Citadel model and server-authoritative keyframe fixture. */
public final class DebugCitadelFly extends EntityCreature implements IAnimatedEntity {

    public static final Animation WING_WAVE = Animation.create(20);
    private static final Animation[] ANIMATIONS = { WING_WAVE };

    private Animation animation = NO_ANIMATION;
    private int animationTick;

    public DebugCitadelFly(World world) {
        super(world);
        setSize(0.8F, 0.8F);
        func_110163_bv();
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public boolean interact(EntityPlayer player) {
        if (!this.worldObj.isRemote && this.animation == NO_ANIMATION)
            AnimationHandler.INSTANCE.sendAnimationMessage(this, WING_WAVE);

        return true;
    }

    @Override
    public int getAnimationTick() {
        return this.animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        this.animationTick = tick;
    }

    @Override
    public Animation getAnimation() {
        return this.animation;
    }

    @Override
    public void setAnimation(Animation animation) {
        this.animation = animation;
    }

    @Override
    public Animation[] getAnimations() {
        return ANIMATIONS;
    }
}
