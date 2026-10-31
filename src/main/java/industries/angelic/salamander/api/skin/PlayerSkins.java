package industries.angelic.salamander.api.skin;

import java.awt.image.BufferedImage;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.compat.wawelauth.WawelAuthCompat;
import industries.angelic.salamander.skin.source.SkinSources;

/**
 * Where Salamander's player rendering gets skins: Wawel Auth's skin info when it is loaded, otherwise vanilla's own
 * downloads, kept at 64x64 by Salamander. Client thread only.
 */
@SideOnly(Side.CLIENT)
public final class PlayerSkins {

    private static Source source;

    private PlayerSkins() {}

    /** The player's current skin; never null. */
    public static PlayerSkin get(AbstractClientPlayer player) {
        return source().get(player);
    }

    public static boolean slim(AbstractClientPlayer player) {
        return source().slim(player);
    }

    /** Whether the wearer shows this part. Live state, not part of a {@link PlayerSkin} snapshot. */
    public static boolean isShown(EntityPlayer player, SkinPart part) {
        return source().isShown(player, part);
    }

    /** The skin texture for a player head of this profile, or null to keep the one vanilla resolved. */
    public static ResourceLocation skull(GameProfile profile) {
        return source().skull(profile);
    }

    /** The 64x64 image of a loaded skin texture, such as a player head's; null when there is none. */
    public static BufferedImage image(ResourceLocation texture) {
        return source().image(texture);
    }

    /** Whether skins come from Salamander's own vanilla downloads rather than from Wawel Auth. */
    public static boolean vanilla() {
        return SkinSources.isVanilla(source());
    }

    private static Source source() {
        if (source == null) {
            Source wawel = WawelAuthCompat.skins();
            source = wawel != null ? wawel : SkinSources.vanilla();
        }
        return source;
    }

    /** One place skins come from. */
    public interface Source {

        PlayerSkin get(AbstractClientPlayer player);

        boolean slim(AbstractClientPlayer player);

        boolean isShown(EntityPlayer player, SkinPart part);

        ResourceLocation skull(GameProfile profile);

        BufferedImage image(ResourceLocation texture);
    }
}
