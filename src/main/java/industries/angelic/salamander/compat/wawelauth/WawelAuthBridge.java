package industries.angelic.salamander.compat.wawelauth;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

import industries.angelic.wawelauth.api.account.AccountInfo;
import industries.angelic.wawelauth.api.account.WawelAccounts;
import industries.angelic.wawelauth.api.skin.PlayerSkin;
import industries.angelic.wawelauth.api.skin.PlayerSkins;
import industries.angelic.wawelauth.wawelcore.data.SkinModel;

/** Wawel Auth's skin and account API. Loaded only when a compatible Wawel Auth is present. */
final class WawelAuthBridge {

    private WawelAuthBridge() {}

    static WawelAuthCompat.SkinPixels skin(AbstractClientPlayer player, ResourceLocation texture) {
        PlayerSkin skin = PlayerSkins.get(player);
        if (!skin.available() || !texture.equals(skin.texture())) return null;
        return new WawelAuthCompat.SkinPixels(skin.revision(), skin::originalImage);
    }

    static WawelAuthUpload.Account activeAccount() {
        AccountInfo account = WawelAccounts.active();
        if (account == null) return null;
        return new WawelAuthUpload.Account(
            account.id(),
            account.profileName(),
            account.profileUuid(),
            account.providerName(),
            account.offline());
    }

    /**
     * Whether Wawel Auth shows its Upload Skin button for this account. Asked by reflection: Wawel Auths before 1.0.6
     * can't say, and their button showed for every account.
     */
    static boolean skinUploadEnabled(WawelAuthUpload.Account account) {
        AccountInfo info = new AccountInfo(account.id, account.name, account.uuid, account.provider, account.offline);
        try {
            Object enabled = WawelAccounts.class.getMethod("skinUploadEnabled", AccountInfo.class)
                .invoke(null, info);
            return !(enabled instanceof Boolean) || (Boolean) enabled;
        } catch (ReflectiveOperationException olderOrFailed) {
            return true;
        }
    }

    /** Completes on the client thread. */
    static CompletableFuture<String> uploadSkin(WawelAuthUpload.Account account, BufferedImage image, boolean slim) {
        AccountInfo info = new AccountInfo(account.id, account.name, account.uuid, account.provider, account.offline);
        return WawelAccounts.uploadSkin(info, image, slim ? SkinModel.SLIM : SkinModel.CLASSIC);
    }
}
