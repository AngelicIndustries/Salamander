package industries.angelic.salamander.compat.wawelauth;

import industries.angelic.wawelauth.api.skin.WawelSkinApi;

/** Probes the installed Wawel Auth for the skin API this build uses. Loaded only when Wawel Auth is present. */
final class WawelAuthApi {

    private WawelAuthApi() {}

    /** False for a Wawel Auth from before the API, or one whose API changed since. */
    static boolean compatible() {
        try {
            return WawelSkinApi.version() == WawelSkinApi.VERSION;
        } catch (LinkageError olderWawelAuth) {
            return false;
        }
    }
}
