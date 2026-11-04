package com.geckolib.animation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.object.EasingType;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationPoint;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.animation.state.ControllerState;
import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.animation.keyframeevent.CustomInstructionKeyframeData;
import com.geckolib.cache.animation.keyframeevent.KeyFrameData;
import com.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import com.geckolib.cache.animation.keyframeevent.SoundKeyframeData;
import com.geckolib.loading.math.MolangContext;

/**
 * Headless GeckoLib animation controller.
 *
 * <p>
 * This owns deterministic timeline and transition state without depending on client rendering classes.
 */
public class AnimationController<T extends GeoAnimatable> {

    private final String name;
    private final AnimationStateHandler<T> stateHandler;
    private final Map<String, RawAnimation> triggerableAnimations = new LinkedHashMap<>();

    private KeyframeEventHandler<T, SoundKeyframeData> soundKeyframeHandler;
    private KeyframeEventHandler<T, ParticleKeyframeData> particleKeyframeHandler;
    private KeyframeEventHandler<T, CustomInstructionKeyframeData> customKeyframeHandler;
    private int transitionTicks;
    private double animationSpeed = 1;
    private boolean additiveAnimations;
    private boolean handlesTriggeredAnimations;
    private EasingType easingOverride;
    private PlayState playState = PlayState.STOP;
    private RawAnimation currentRawAnimation;
    private RawAnimation initializedRawAnimation;
    private AnimationPoint animationPoint;
    private int stageIndex;
    private double stageTime;
    private double lastAnimatableAge = Double.NaN;
    private boolean playingTriggeredAnimation;
    private boolean finished;
    private boolean transitioning;
    private boolean resetting;
    private boolean transitionStartedThisTick;
    private double transitionElapsedTicks;
    private Map<String, BoneSnapshot> transitionStartPose = Collections.emptyMap();
    private Map<String, BoneSnapshot> lastEvaluatedPose = Collections.emptyMap();
    private BakedAnimations lastAnimations;

    public AnimationController(AnimationStateHandler<T> stateHandler) {
        this("Default", 0, stateHandler);
    }

    public AnimationController(String name, AnimationStateHandler<T> stateHandler) {
        this(name, 0, stateHandler);
    }

    public AnimationController(String name, int transitionTicks, AnimationStateHandler<T> stateHandler) {
        this.name = name;
        this.transitionTicks = Math.max(0, transitionTicks);
        this.stateHandler = stateHandler;
    }

    public String getName() {
        return this.name;
    }

    public PlayState getPlayState() {
        return this.playState;
    }

    public int getTransitionTicks() {
        return this.transitionTicks;
    }

    public double getAnimationSpeed() {
        return this.animationSpeed;
    }

    public double getCurrentTimelineTime() {
        return this.animationPoint == null ? -1 : this.stageTime;
    }

    public double getCurrentAnimationTime() {
        return this.animationPoint == null ? 0 : this.animationPoint.animTime();
    }

    public AnimationPoint getCurrentAnimationPoint() {
        return this.animationPoint;
    }

    public RawAnimation getCurrentRawAnimation() {
        return this.currentRawAnimation;
    }

    public boolean isPlayingTriggeredAnimation() {
        return this.playingTriggeredAnimation && this.animationPoint != null;
    }

    public boolean isTriggeredAnimation(String animationName) {
        RawAnimation animation = this.triggerableAnimations.get(animationName);

        return this.playingTriggeredAnimation && animation != null && animation.equals(this.currentRawAnimation);
    }

    public boolean hasAnimationFinished() {
        return this.finished;
    }

    public boolean isAnimatingBones() {
        return this.animationPoint != null || this.resetting;
    }

    public boolean isTransitioning() {
        return this.transitioning;
    }

    public boolean isAdditive() {
        return this.additiveAnimations;
    }

    public AnimationController<T> setAnimationSpeed(double speed) {
        this.animationSpeed = speed;

        return this;
    }

    public AnimationController<T> setTransitionTicks(int ticks) {
        this.transitionTicks = Math.max(0, ticks);

        return this;
    }

    public AnimationController<T> additiveAnimations() {
        this.additiveAnimations = true;

        return this;
    }

    public AnimationController<T> setOverrideEasingType(EasingType easingType) {
        this.easingOverride = easingType;

        return this;
    }

    public AnimationController<T> receiveTriggeredAnimations() {
        this.handlesTriggeredAnimations = true;

        return this;
    }

    public AnimationController<T> triggerableAnim(String name, RawAnimation animation) {
        this.triggerableAnimations.put(name, animation);

        return this;
    }

    public AnimationController<T> setSoundKeyframeHandler(KeyframeEventHandler<T, SoundKeyframeData> soundHandler) {
        this.soundKeyframeHandler = soundHandler;

        return this;
    }

    public AnimationController<T> setParticleKeyframeHandler(
        KeyframeEventHandler<T, ParticleKeyframeData> particleHandler) {
        this.particleKeyframeHandler = particleHandler;

        return this;
    }

    public AnimationController<T> setCustomInstructionKeyframeHandler(
        KeyframeEventHandler<T, CustomInstructionKeyframeData> customInstructionHandler) {
        this.customKeyframeHandler = customInstructionHandler;

        return this;
    }

    public void setAnimation(RawAnimation rawAnimation) {
        if (rawAnimation == null || rawAnimation.getStageCount() == 0)
            throw new IllegalArgumentException("AnimationController cannot play an empty animation");

        if (rawAnimation.equals(this.currentRawAnimation)) return;

        beginTransition(this.lastEvaluatedPose, false);
        this.currentRawAnimation = rawAnimation;
        this.initializedRawAnimation = null;
        this.playingTriggeredAnimation = false;
        this.finished = false;
    }

    /**
     * Whether every stage of the animation exists in the animations this controller last ticked with. True before the
     * first tick, when they aren't known yet. GeckoLib 4 kept playing the current animation when asked for a missing
     * one; callers that need that can check this first.
     */
    public boolean canPlay(RawAnimation rawAnimation) {
        if (rawAnimation == null || this.lastAnimations == null) return true;

        for (RawAnimation.Stage stage : rawAnimation.getAnimationStages()) {
            if (resolveAnimation(stage, this.lastAnimations) == null) return false;
        }

        return true;
    }

    public boolean triggerAnimation(String animationName) {
        RawAnimation animation = this.triggerableAnimations.get(animationName);

        if (animation == null) return false;

        beginTransition(this.lastEvaluatedPose, false);
        this.currentRawAnimation = animation;
        this.initializedRawAnimation = null;
        this.playingTriggeredAnimation = true;
        this.playState = PlayState.CONTINUE;
        this.finished = false;

        return true;
    }

    public boolean stopTriggeredAnimation() {
        if (!this.playingTriggeredAnimation) return false;

        reset();

        return true;
    }

    public void setAnimationTime(double animationTime) {
        if (animationTime < 0) throw new IllegalArgumentException("Animation time cannot be negative");

        this.stageTime = animationTime;

        if (this.animationPoint != null) this.animationPoint = this.animationPoint.createNext(animationTime);
    }

    public void reset() {
        this.playState = PlayState.STOP;
        this.currentRawAnimation = null;
        this.initializedRawAnimation = null;
        this.animationPoint = null;
        this.stageIndex = 0;
        this.stageTime = 0;
        this.playingTriggeredAnimation = false;
        this.finished = false;
        this.transitioning = false;
        this.resetting = false;
        this.transitionStartedThisTick = false;
        this.transitionElapsedTicks = 0;
        this.transitionStartPose = Collections.emptyMap();
        this.lastEvaluatedPose = Collections.emptyMap();
    }

    /** Advance this controller using an animatable age measured in Minecraft ticks. */
    public AnimationPoint tick(T animatable, AnimatableManager<T> manager, BakedAnimations animations,
        double animatableAge, MolangContext molangContext) {
        MolangContext context = molangContext == null ? MolangContext.EMPTY : molangContext;
        double tickDelta = calculateTickDelta(animatableAge);
        this.lastAnimations = animations;

        if (!this.playingTriggeredAnimation || this.handlesTriggeredAnimations) {
            this.playState = this.stateHandler.handle(new AnimationTest<>(animatable, manager, this, context));
        }

        if (this.playState == PlayState.STOP) {
            if (!this.resetting && this.animationPoint != null) {
                Map<String, BoneSnapshot> resetPose = this.lastEvaluatedPose;

                if (resetPose.isEmpty()) {
                    ControllerState state = new ControllerState(context)
                        .setAnimationTime(this.animationPoint.animTime());

                    resetPose = AnimationProcessor.evaluate(this.animationPoint, state);
                }

                this.lastEvaluatedPose = copyPose(resetPose);
                beginTransition(resetPose, true);
                this.currentRawAnimation = null;
                this.initializedRawAnimation = null;
                this.animationPoint = null;
                this.stageTime = 0;
                this.finished = false;
            }

            advanceTransition(tickDelta);

            return null;
        }

        if (this.currentRawAnimation == null) {
            advanceTransition(tickDelta);

            return null;
        }

        if (this.initializedRawAnimation != this.currentRawAnimation) {
            this.initializedRawAnimation = this.currentRawAnimation;
            this.stageIndex = 0;
            this.stageTime = 0;
            this.finished = false;
            initializeCurrentStage(animatable, animations);
        }

        if (this.animationPoint == null) return null;

        if (this.playState != PlayState.PAUSE && !this.finished) advanceTimeline(
            animatable,
            manager,
            tickDelta / 20d * this.animationSpeed,
            animations,
            context,
            partialTick(animatableAge));

        advanceTransition(tickDelta);

        return this.animationPoint;
    }

    public Map<String, BoneSnapshot> evaluateCurrentPose(MolangContext molangContext) {
        Map<String, BoneSnapshot> targetPose;

        if (this.resetting || this.animationPoint == null) {
            targetPose = Collections.emptyMap();
        } else {
            ControllerState state = new ControllerState(molangContext).setAnimationTime(this.animationPoint.animTime());

            targetPose = AnimationProcessor.evaluate(this.animationPoint, state);
        }

        Map<String, BoneSnapshot> result = this.transitioning
            ? interpolatePose(this.transitionStartPose, targetPose, getTransitionProgress())
            : copyPose(targetPose);

        this.lastEvaluatedPose = copyPose(result);

        return Collections.unmodifiableMap(result);
    }

    private double calculateTickDelta(double animatableAge) {
        if (Double.isNaN(this.lastAnimatableAge)) {
            this.lastAnimatableAge = animatableAge;
            return 0;
        }

        double delta = animatableAge - this.lastAnimatableAge;

        this.lastAnimatableAge = animatableAge;

        if (delta < -1) return 0;

        return delta;
    }

    private void initializeCurrentStage(T animatable, BakedAnimations animations) {
        while (this.stageIndex < this.currentRawAnimation.getStageCount()) {
            RawAnimation.Stage rawStage = this.currentRawAnimation.getAnimationStages()
                .get(this.stageIndex);
            Animation animation = resolveAnimation(rawStage, animations);

            if (animation != null) {
                this.animationPoint = AnimationPoint.createFor(animation, this.easingOverride, rawStage.loopType(), 0);
                validateKeyframeHandlers(animatable, animation);
                return;
            }

            this.stageIndex++;
        }

        this.animationPoint = null;
        this.finished = true;
    }

    private void advanceTimeline(T animatable, AnimatableManager<T> manager, double timeAdvanced,
        BakedAnimations animations, MolangContext molangContext, float partialTick) {
        if (timeAdvanced == 0 || this.animationPoint == null) return;

        double previousStageTime = this.stageTime;

        this.stageTime += timeAdvanced;

        while (this.animationPoint != null) {
            Animation animation = this.animationPoint.animation();
            double length = animation.length();

            if (this.stageTime < length) {
                triggerKeyframeMarkersBetween(
                    animatable,
                    manager,
                    animation,
                    previousStageTime,
                    this.stageTime,
                    partialTick);
                this.animationPoint = this.animationPoint.createNext(this.stageTime);
                return;
            }

            triggerKeyframeMarkersBetween(animatable, manager, animation, previousStageTime, length, partialTick);

            LoopType loopType = this.animationPoint.loopType() == LoopType.DEFAULT ? animation.loopType()
                : this.animationPoint.loopType();

            if (loopType == LoopType.LOOP || loopType.shouldPlayAgain(animation)) {
                if (length == 0) {
                    this.stageTime = 0;
                } else {
                    this.stageTime %= length;
                    triggerKeyframeMarkersBetween(animatable, manager, animation, 0, this.stageTime, partialTick);
                }

                this.animationPoint = AnimationPoint
                    .createFor(animation, this.easingOverride, loopType, this.stageTime);
                return;
            }

            if (loopType == LoopType.HOLD_ON_LAST_FRAME) {
                this.stageTime = length;
                this.animationPoint = this.animationPoint.createNext(length);
                this.finished = true;
                return;
            }

            this.stageTime -= length;
            this.stageIndex++;

            if (this.stageIndex >= this.currentRawAnimation.getStageCount()) {
                this.stageTime = length;
                this.animationPoint = this.animationPoint.createNext(length);
                this.finished = true;
                return;
            }

            ControllerState state = new ControllerState(molangContext).setAnimationTime(length);

            beginTransition(AnimationProcessor.evaluate(this.animationPoint.createNext(length), state), false);
            initializeCurrentStage(animatable, animations);
            previousStageTime = 0;
        }
    }

    private void triggerKeyframeMarkersBetween(T animatable, AnimatableManager<T> manager, Animation animation,
        double fromTime, double toTime, float partialTick) {
        Animation.KeyframeMarkers markers = animation.keyframeMarkers();

        triggerKeyframeMarkers(
            animatable,
            manager,
            markers.sounds(),
            fromTime,
            toTime,
            this.soundKeyframeHandler,
            partialTick);
        triggerKeyframeMarkers(
            animatable,
            manager,
            markers.particles(),
            fromTime,
            toTime,
            this.particleKeyframeHandler,
            partialTick);
        triggerKeyframeMarkers(
            animatable,
            manager,
            markers.customInstructions(),
            fromTime,
            toTime,
            this.customKeyframeHandler,
            partialTick);
    }

    private <E extends KeyFrameData> void triggerKeyframeMarkers(T animatable, AnimatableManager<T> manager,
        E[] markers, double fromTime, double toTime, KeyframeEventHandler<T, E> handler, float partialTick) {
        if (handler == null || toTime <= fromTime) return;

        for (E marker : markers) {
            if (marker.getTime() > toTime) break;

            if (marker.getTime() > fromTime || fromTime == 0) {
                handler.handle(new KeyFrameEvent<>(animatable, manager, this, marker, partialTick));
            }
        }
    }

    private void validateKeyframeHandlers(T animatable, Animation animation) {
        Animation.KeyframeMarkers markers = animation.keyframeMarkers();

        if (markers.sounds().length > 0 && this.soundKeyframeHandler == null) {
            com.geckolib.GeckoLibConstants.LOGGER.warn(
                "Animation controller {} for {} loaded {} with sound keyframes but no sound handler",
                this.name,
                animatable.getClass()
                    .getName(),
                animation.name());
        }

        if (markers.particles().length > 0 && this.particleKeyframeHandler == null) {
            com.geckolib.GeckoLibConstants.LOGGER.warn(
                "Animation controller {} for {} loaded {} with particle keyframes but no particle handler",
                this.name,
                animatable.getClass()
                    .getName(),
                animation.name());
        }

        if (markers.customInstructions().length > 0 && this.customKeyframeHandler == null) {
            com.geckolib.GeckoLibConstants.LOGGER.warn(
                "Animation controller {} for {} loaded {} with custom instruction keyframes but no custom handler",
                this.name,
                animatable.getClass()
                    .getName(),
                animation.name());
        }
    }

    private static float partialTick(double animatableAge) {
        return (float) (animatableAge - Math.floor(animatableAge));
    }

    private void beginTransition(Map<String, BoneSnapshot> startPose, boolean resetting) {
        this.resetting = resetting;

        if (this.transitionTicks == 0) {
            this.transitioning = false;
            this.resetting = false;
            this.transitionStartedThisTick = false;
            this.transitionElapsedTicks = 0;
            this.transitionStartPose = Collections.emptyMap();

            if (resetting) this.lastEvaluatedPose = Collections.emptyMap();

            return;
        }

        this.transitioning = true;
        this.transitionStartedThisTick = true;
        this.transitionElapsedTicks = 0;
        this.transitionStartPose = copyPose(startPose);
    }

    private void advanceTransition(double tickDelta) {
        if (!this.transitioning) return;

        if (this.transitionStartedThisTick) {
            this.transitionStartedThisTick = false;
            return;
        }

        this.transitionElapsedTicks = Math.min(this.transitionTicks, this.transitionElapsedTicks + tickDelta);

        if (this.transitionElapsedTicks < this.transitionTicks) return;

        this.transitioning = false;
        this.transitionStartPose = Collections.emptyMap();

        if (this.resetting) {
            this.resetting = false;
            this.lastEvaluatedPose = Collections.emptyMap();
        }
    }

    private float getTransitionProgress() {
        return this.transitionTicks == 0 ? 1 : (float) (this.transitionElapsedTicks / this.transitionTicks);
    }

    private static Map<String, BoneSnapshot> interpolatePose(Map<String, BoneSnapshot> start,
        Map<String, BoneSnapshot> target, float progress) {
        Map<String, BoneSnapshot> result = new LinkedHashMap<>();

        for (String boneName : start.keySet()) {
            result.put(boneName, interpolateSnapshot(start.get(boneName), target.get(boneName), progress));
        }

        for (String boneName : target.keySet()) {
            if (!result.containsKey(boneName))
                result.put(boneName, interpolateSnapshot(null, target.get(boneName), progress));
        }

        return result;
    }

    private static BoneSnapshot interpolateSnapshot(BoneSnapshot start, BoneSnapshot target, float progress) {
        String boneName = start == null ? target.getBoneName() : start.getBoneName();
        BoneSnapshot from = start == null ? BoneSnapshot.create(boneName) : start;
        BoneSnapshot to = target == null ? BoneSnapshot.create(boneName) : target;

        return BoneSnapshot.create(boneName)
            .setScale(
                lerp(from.getScaleX(), to.getScaleX(), progress),
                lerp(from.getScaleY(), to.getScaleY(), progress),
                lerp(from.getScaleZ(), to.getScaleZ(), progress))
            .setRotation(
                lerpRotation(from.getRotX(), to.getRotX(), progress),
                lerpRotation(from.getRotY(), to.getRotY(), progress),
                lerpRotation(from.getRotZ(), to.getRotZ(), progress))
            .setTranslation(
                lerp(from.getTranslateX(), to.getTranslateX(), progress),
                lerp(from.getTranslateY(), to.getTranslateY(), progress),
                lerp(from.getTranslateZ(), to.getTranslateZ(), progress))
            .skipRender(progress < 0.5f ? from.isHidden() : to.isHidden())
            .skipChildrenRender(progress < 0.5f ? from.areChildrenHidden() : to.areChildrenHidden());
    }

    private static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private static float lerpRotation(float start, float end, float progress) {
        float delta = (float) Math.atan2(Math.sin(end - start), Math.cos(end - start));

        return start + delta * progress;
    }

    private static Map<String, BoneSnapshot> copyPose(Map<String, BoneSnapshot> pose) {
        if (pose.isEmpty()) return Collections.emptyMap();

        Map<String, BoneSnapshot> copy = new LinkedHashMap<>(pose.size());

        for (Map.Entry<String, BoneSnapshot> entry : pose.entrySet()) {
            copy.put(
                entry.getKey(),
                entry.getValue()
                    .copy());
        }

        return copy;
    }

    private Animation resolveAnimation(RawAnimation.Stage stage, BakedAnimations animations) {
        if (RawAnimation.Stage.WAIT.equals(stage.animationName()))
            return Animation.generateWaitAnimation(stage.waitTicks());

        return animations.getAnimation(stage.animationName());
    }

    @FunctionalInterface
    public interface AnimationStateHandler<A extends GeoAnimatable> {

        PlayState handle(AnimationTest<A> animationTest);
    }

    @FunctionalInterface
    public interface KeyframeEventHandler<A extends GeoAnimatable, E extends KeyFrameData> {

        void handle(KeyFrameEvent<A, E> event);
    }
}
