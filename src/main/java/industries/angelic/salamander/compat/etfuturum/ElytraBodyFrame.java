package industries.angelic.salamander.compat.etfuturum;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import industries.angelic.salamander.api.player.BodyFrames;

/**
 * Et Futurum's elytra, drawn in the frame of a body another mod bends (see {@link BodyFrames}). Shared by the layer
 * and model mixins: the player drawing in a body frame, whose wings then keep their standing pose.
 */
public final class ElytraBodyFrame {

    private static EntityLivingBase framed;

    private ElytraBodyFrame() {}

    /** Before the wings draw: moves into a provider's body frame if one draws this player. */
    public static void begin(EntityLivingBase entity, float scale) {
        framed = entity instanceof EntityPlayer && BodyFrames.apply((EntityPlayer) entity, scale) ? entity : null;
    }

    public static void end() {
        framed = null;
    }

    /** Whether the elytra should take vanilla's crouch pose for {@code entity}. */
    public static boolean crouches(boolean sneaking, Object entity) {
        return sneaking && entity != framed;
    }
}
