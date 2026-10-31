package industries.angelic.salamander.skin.source;

import industries.angelic.salamander.api.skin.PlayerSkins;

/** The skin sources Salamander implements itself. */
public final class SkinSources {

    private SkinSources() {}

    public static PlayerSkins.Source vanilla() {
        return new VanillaSkins();
    }

    public static boolean isVanilla(PlayerSkins.Source source) {
        return source instanceof VanillaSkins;
    }
}
