package industries.angelic.salamander.api.cem;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.cem.animation.CemExpressionParser;
import industries.angelic.salamander.cem.animation.CemExtensions;
import industries.angelic.salamander.cem.client.CemAnimationControl;
import industries.angelic.salamander.cem.client.CemFaces;
import industries.angelic.salamander.cem.client.CemModded;
import industries.angelic.salamander.cem.client.CemRuntime;
import industries.angelic.salamander.cem.client.CemTargets;
import industries.angelic.salamander.cem.model.CemModel;

/**
 * Lets other mods extend the resource-pack entity models Salamander draws (OptiFine CEM and EMF), as EMF's animation
 * API does. Without any of it, a mod's living entity already takes the model {@code assets/<modid>/optifine/cem/
 * <entity_name>.jem}, with parts named after its model's fields; Salamander's model exporter writes them out.
 * <p>
 * Register on the client during initialization: models compile when resource packs load, and see only what is
 * registered by then. Draw-time calls arrive on the render thread.
 */
@SideOnly(Side.CLIENT)
public final class CemApi {

    private static final Pattern NAME = Pattern.compile("[a-z_][a-z0-9_]*");

    private CemApi() {}

    /**
     * Draws entities of this exact class with the pack model {@code model}: {@code "mymod:wild_boar"} for
     * {@code assets/mymod/optifine/cem/wild_boar.jem}, or a vanilla name such as {@code "zombie"} to share its packs.
     */
    public static void registerEntity(String entityClass, String model) {
        CemTargets.register(entityClass, model);
    }

    /**
     * Names a model's parts for pack models, in place of the names of its fields: OptiFine's names for a model that
     * copies a vanilla one, say. The map holds the model's own part objects; it is read once per model instance.
     */
    public static void registerParts(Class<? extends ModelBase> modelClass,
        Function<ModelBase, Map<String, ModelRenderer>> parts) {
        CemModded.registerParts(modelClass, parts);
    }

    /**
     * An animation variable named {@code name}, read for the entity or block entity being drawn (possibly null).
     * Lower case letters, digits and underscores, and no name expressions already know.
     */
    public static void registerVariable(String modId, String name, ToDoubleFunction<Object> value) {
        check(modId, name, value);
        if (CemModel.INPUTS.contains(name) || CemModel.RENDER_PROPERTIES.contains(name)
            || CemExpressionParser.builtIn(name)) throw new IllegalArgumentException(name + " is already a CEM name");
        CemExtensions.variable(name, value);
        Salamander.LOG.info("CEM variable '{}' registered by {}", name, modId);
    }

    /**
     * An animation function named {@code name}: a public static method taking doubles and returning a double, called
     * directly from compiled animations.
     */
    public static void registerFunction(String modId, String name, Method method) {
        check(modId, name, method);
        if (CemExpressionParser.builtIn(name) || CemExtensions.isVariable(name))
            throw new IllegalArgumentException(name + " is already a CEM name");
        CemExtensions.function(name, method);
        Salamander.LOG.info("CEM function '{}' registered by {}", name, modId);
    }

    /**
     * Runs around every pack model animation, as EMF's animation hooks: any hook can cancel a run, and every hook still
     * hears of it. A cancelled run leaves the model in its vanilla pose for that frame.
     */
    public abstract static class AnimationHook {

        /**
         * @param subject     the entity or block entity being animated
         * @param model       the pack model, as {@code minecraft:optifine/cem/zombie.jem}
         * @param pausedParts the parts whose animations were asked to pause for this subject
         * @return false to cancel this run
         */
        public boolean onAnimationStart(Object subject, String model, Set<String> pausedParts, boolean cancelled) {
            return true;
        }

        public void onAnimationEnd(Object subject, String model, Set<String> pausedParts, boolean cancelled) {}

        /**
         * Before an armor model takes its wearer's bone poses, as EMF copies a biped's pose.
         *
         * @return false to keep the armor model's own pose
         */
        public boolean onPoseCopy(Object subject, String model, boolean cancelled) {
            return true;
        }
    }

    public static void registerAnimationHook(AnimationHook hook) {
        if (hook == null) throw new IllegalArgumentException("null animation hook");
        CemAnimationControl.hook(new CemAnimationControl.Hook() {

            @Override
            public boolean start(Object subject, String model, Set<String> pausedParts, boolean cancelled) {
                return hook.onAnimationStart(subject, model, pausedParts, cancelled);
            }

            @Override
            public void end(Object subject, String model, Set<String> pausedParts, boolean cancelled) {
                hook.onAnimationEnd(subject, model, pausedParts, cancelled);
            }

            @Override
            public boolean poseCopy(Object subject, String model, boolean cancelled) {
                return hook.onPoseCopy(subject, model, cancelled);
            }
        });
    }

    /** Pauses the animations of every entity or block entity this holds for, which keep their vanilla pose. */
    public static void registerPauseCondition(Predicate<Object> shouldPause) {
        if (shouldPause == null) throw new IllegalArgumentException("null pause condition");
        CemAnimationControl.pauseCondition(shouldPause);
    }

    /**
     * Pauses a pack model's animations for this entity or block entity: its parts keep their vanilla pose.
     *
     * @return false for anything else
     */
    public static boolean pauseAnimations(Object entityOrTileEntity) {
        return CemAnimationControl.pause(entityOrTileEntity, true);
    }

    /**
     * Pauses only these parts' animations, by their pack model names ({@code head}, {@code left_arm}); the rest
     * animate. Replaces parts paused before.
     */
    public static boolean pauseAnimations(Object entityOrTileEntity, String... parts) {
        return CemAnimationControl.pauseParts(entityOrTileEntity, parts);
    }

    /** Resumes what {@link #pauseAnimations} paused, whole or part by part. */
    public static boolean resumeAnimations(Object entityOrTileEntity) {
        return CemAnimationControl.pause(entityOrTileEntity, false);
    }

    /** Draws every entity or block entity this holds for with its vanilla model, whatever the packs hold. */
    public static void registerVanillaModelCondition(Predicate<Object> useVanillaModel) {
        if (useVanillaModel == null) throw new IllegalArgumentException("null vanilla model condition");
        CemAnimationControl.vanillaCondition(useVanillaModel);
    }

    public static boolean lockToVanillaModel(Object entityOrTileEntity) {
        return CemAnimationControl.lockVanilla(entityOrTileEntity, true);
    }

    public static boolean unlockFromVanillaModel(Object entityOrTileEntity) {
        return CemAnimationControl.lockVanilla(entityOrTileEntity, false);
    }

    /** The entity or block entity whose model is being drawn now, or null. */
    public static Object currentSubject() {
        return CemRuntime.subject();
    }

    /** What a player's pack model covers, for a mod that draws the player itself. */
    public enum PlayerPackCoverage {
        /** No pack model draws the player. */
        NONE,
        /**
         * The pack keeps the vanilla body and adds only to the head, animating only that, as Just Expressions does:
         * {@link #drawPlayerPackFace} draws that face on another model's head.
         */
        FACE,
        /** The pack changes or animates more than the head, as Fresh Moves does: it draws the whole player. */
        BODY
    }

    /** What the pack model the player would draw with now covers; NONE for anything but a player. */
    public static PlayerPackCoverage playerPackCoverage(Entity player) {
        return PlayerPackCoverage.valueOf(
            CemFaces.coverage(player)
                .name());
    }

    /** The name of the resource pack the player's pack model comes from, or null when none draws the player. */
    public static String playerPackSource(Entity player) {
        return CemFaces.packName(player);
    }

    /**
     * Draws the face of the player's pack model at the current GL matrix, which must be the caller's head bone: the
     * vanilla head's frame, at its pivot and turned as the head. The pack's animations run for the player as usual;
     * its head's own turn and the vanilla head and hat cubes are left to the caller. Pass what the caller's model got
     * to draw (head yaw relative to the body, in degrees).
     *
     * @return whether a face was drawn: only for {@link PlayerPackCoverage#FACE}
     */
    public static boolean drawPlayerPackFace(Entity player, float limbSwing, float limbSpeed, float age, float headYaw,
        float headPitch, float scale) {
        return CemFaces.draw(player, limbSwing, limbSpeed, age, headYaw, headPitch, scale);
    }

    /**
     * The name of the resource pack whose model would draw {@code subject} with {@code model} and {@code texture}, as
     * Salamander picks it when that model draws; null when none would.
     */
    public static String packModelSource(Object subject, ModelBase model, ResourceLocation texture) {
        return CemRuntime.packModelSource(subject, model, texture);
    }

    /**
     * Keeps pack models off {@code model}, which then draws as its own code draws it whatever the packs hold for its
     * entities: for a model a mod draws in place of the vanilla one, such as Even Mo' Bends' player, that still takes
     * a pack's face through {@link #drawPlayerPackFace}.
     */
    public static void excludeModel(ModelBase model) {
        CemRuntime.exclude(model);
    }

    private static void check(String modId, String name, Object value) {
        if (modId == null || value == null
            || name == null
            || !NAME.matcher(name)
                .matches())
            throw new IllegalArgumentException("Invalid CEM registration '" + name + "' from " + modId);
    }
}
