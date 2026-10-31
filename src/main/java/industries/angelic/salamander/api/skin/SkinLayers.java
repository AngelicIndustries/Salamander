package industries.angelic.salamander.api.skin;

import net.minecraft.entity.Entity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.compat.ears.EarsTransformerAccess;
import industries.angelic.salamander.skin.layers.LayerDraws;

/**
 * Entry point for drawing skin overlay layers (hat, jacket, sleeves, pants). Client thread only.
 *
 * <pre>
 * try (LayerDraw draw = SkinLayers.begin(player, DrawKind.WORLD, scale)) {
 *     // draw the base model, then for each overlay piece, with the part's transform applied:
 *     draw.submit(segment);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public final class SkinLayers {

    private SkinLayers() {}

    /**
     * Opens a draw for an entity's layers. Players get the full treatment; any other entity drawn with a player model
     * (Botania's Gaia Guardian, a Twilight Forest giant) gets flat layers with every part shown.
     */
    public static LayerDraw begin(Entity entity, DrawKind kind, float scale) {
        return LayerDraws.begin(entity, kind, scale);
    }

    /**
     * The innermost open draw, or null. Lets code that runs inside a host's model render, such as a model replacement,
     * suppress the layers of exactly that draw.
     */
    public static LayerDraw current() {
        return LayerDraws.current();
    }

    /**
     * Whether Ears, when installed, puts its features on the bones skin layers are submitted on and leaves the second
     * layer to them. A renderer that submits all six overlay parts on its own bones can then keep its model with Ears.
     */
    public static boolean earsFollowLayers() {
        return EarsTransformerAccess.opened();
    }
}
