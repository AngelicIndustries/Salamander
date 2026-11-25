package industries.angelic.salamander.cem.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import industries.angelic.salamander.Salamander;

/**
 * What other mods ask of pack models through the API, as through EMF's: entities whose animations pause, in whole or
 * part by part, entities kept on their vanilla model, and hooks around every animation run. Entities are known by
 * their UUID as the client sees it, which 1.7.10 does not share with the server for anything but players, and block
 * entities by their place.
 */
public final class CemAnimationControl {

    /** Runs around a pack model's animation; see the API's hook. */
    public interface Hook {

        boolean start(Object subject, String model, Set<String> pausedParts, boolean cancelled);

        void end(Object subject, String model, Set<String> pausedParts, boolean cancelled);

        boolean poseCopy(Object subject, String model, boolean cancelled);
    }

    private static final Set<Object> PAUSED = ConcurrentHashMap.newKeySet();
    private static final Map<Object, Set<String>> PAUSED_PARTS = new ConcurrentHashMap<>();
    private static final Set<Object> VANILLA = ConcurrentHashMap.newKeySet();
    private static final List<Predicate<Object>> PAUSE_CONDITIONS = new CopyOnWriteArrayList<>();
    private static final List<Predicate<Object>> VANILLA_CONDITIONS = new CopyOnWriteArrayList<>();
    private static final List<Hook> HOOKS = new CopyOnWriteArrayList<>();

    private CemAnimationControl() {}

    /** An entity's UUID, a block entity's place, or null for anything else. */
    static Object key(Object subject) {
        if (subject instanceof Entity) return ((Entity) subject).getUniqueID();
        if (subject instanceof TileEntity) {
            TileEntity tile = (TileEntity) subject;
            int dimension = tile.getWorldObj() == null || tile.getWorldObj().provider == null ? 0
                : tile.getWorldObj().provider.dimensionId;
            return Arrays.asList(dimension, tile.xCoord, tile.yCoord, tile.zCoord);
        }
        return null;
    }

    public static boolean pause(Object subject, boolean paused) {
        Object key = key(subject);
        if (key == null) return false;
        if (paused) PAUSED.add(key);
        else {
            PAUSED.remove(key);
            PAUSED_PARTS.remove(key);
        }
        return true;
    }

    public static boolean pauseParts(Object subject, String... parts) {
        Object key = key(subject);
        if (key == null || parts == null || parts.length == 0) return false;
        PAUSED_PARTS.put(key, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(parts))));
        return true;
    }

    public static boolean lockVanilla(Object subject, boolean locked) {
        Object key = key(subject);
        if (key == null) return false;
        if (locked) VANILLA.add(key);
        else VANILLA.remove(key);
        return true;
    }

    public static void pauseCondition(Predicate<Object> condition) {
        PAUSE_CONDITIONS.add(condition);
    }

    public static void vanillaCondition(Predicate<Object> condition) {
        VANILLA_CONDITIONS.add(condition);
    }

    public static void hook(Hook hook) {
        HOOKS.add(hook);
    }

    /** Whether this subject draws with its vanilla model, whatever the packs hold. */
    public static boolean vanilla(Object subject) {
        if (VANILLA.isEmpty() && VANILLA_CONDITIONS.isEmpty() || subject == null) return false;
        Object key = key(subject);
        return key != null && VANILLA.contains(key) || any(VANILLA_CONDITIONS, subject);
    }

    static boolean paused(Object subject) {
        if (PAUSED.isEmpty() && PAUSE_CONDITIONS.isEmpty() || subject == null) return false;
        Object key = key(subject);
        return key != null && PAUSED.contains(key) || any(PAUSE_CONDITIONS, subject);
    }

    /** The parts whose animations pause for this subject, or an empty set. */
    static Set<String> pausedParts(Object subject) {
        if (PAUSED_PARTS.isEmpty() || subject == null) return Collections.emptySet();
        Object key = key(subject);
        Set<String> parts = key == null ? null : PAUSED_PARTS.get(key);
        return parts == null ? Collections.emptySet() : parts;
    }

    static boolean hooked() {
        return !HOOKS.isEmpty();
    }

    /** Runs every hook's start; true when one cancels the animation. */
    static boolean start(Object subject, String model, Set<String> pausedParts) {
        boolean cancelled = false;
        for (Hook hook : HOOKS) try {
            if (!hook.start(subject, model, pausedParts, cancelled)) cancelled = true;
        } catch (RuntimeException exception) {
            Salamander.LOG.warn("A CEM animation hook failed: {}", exception.toString());
        }
        return cancelled;
    }

    static void end(Object subject, String model, Set<String> pausedParts, boolean cancelled) {
        for (Hook hook : HOOKS) try {
            hook.end(subject, model, pausedParts, cancelled);
        } catch (RuntimeException exception) {
            Salamander.LOG.warn("A CEM animation hook failed: {}", exception.toString());
        }
    }

    /** Whether armor may take its wearer's pose, as the hooks decide. */
    static boolean poseCopy(Object subject, String model) {
        boolean cancelled = false;
        for (Hook hook : HOOKS) try {
            if (!hook.poseCopy(subject, model, cancelled)) cancelled = true;
        } catch (RuntimeException exception) {
            Salamander.LOG.warn("A CEM animation hook failed: {}", exception.toString());
        }
        return !cancelled;
    }

    private static boolean any(List<Predicate<Object>> conditions, Object subject) {
        for (Predicate<Object> condition : conditions) try {
            if (condition.test(subject)) return true;
        } catch (RuntimeException exception) {
            // As EMF: a failing condition does not hold.
        }
        return false;
    }
}
