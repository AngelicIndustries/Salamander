package industries.angelic.salamander.api.player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.entity.player.EntityPlayer;

import industries.angelic.salamander.Salamander;

/**
 * For mods that bend the player's body, such as Even Mo' Bends: what other mods draw on the back from the model's root
 * with vanilla's own pose, Et Futurum's elytra for one, follows the bent body instead. A provider moves the GL frame
 * from the model's root to the body's, as vanilla's body would stand at the root, when it is the one drawing that
 * player; the elytra then also drops the crouch pose it would add for vanilla's tilted back.
 */
public final class BodyFrames {

    public interface Provider {

        /**
         * When this provider draws {@code player} right now, applies the GL transform from the model's root to its
         * body's frame (origin where vanilla's body pivots, at the neck), in model units of {@code scale}, and returns
         * true. Otherwise changes nothing and returns false.
         */
        boolean apply(EntityPlayer player, float scale);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private BodyFrames() {}

    public static void register(Provider provider) {
        PROVIDERS.add(provider);
    }

    /** Applies the first provider that draws {@code player}; true when one did. */
    public static boolean apply(EntityPlayer player, float scale) {
        for (Provider provider : PROVIDERS) {
            try {
                if (provider.apply(player, scale)) return true;
            } catch (RuntimeException | LinkageError failed) {
                Salamander.LOG.warn("A body frame provider failed: {}", failed.toString());
            }
        }
        return false;
    }
}
