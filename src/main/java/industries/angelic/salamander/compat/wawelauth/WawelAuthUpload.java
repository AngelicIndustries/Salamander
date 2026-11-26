package industries.angelic.salamander.compat.wawelauth;

import java.awt.image.BufferedImage;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import net.minecraft.client.resources.I18n;

/**
 * A skin upload to the active Wawel Auth account, for the expression editor. This facade holds no Wawel Auth types;
 * the bridge using its API loads only when a compatible Wawel Auth is present.
 */
public final class WawelAuthUpload {

    private WawelAuthUpload() {}

    /** The account Wawel Auth plays with, or null without one or without Wawel Auth. */
    public static Account activeAccount() {
        if (!WawelAuthCompat.isLoaded()) return null;
        try {
            return WawelAuthBridge.activeAccount();
        } catch (LinkageError olderWawelAuth) {
            return null;
        }
    }

    /** Whether Wawel Auth offers skin uploads for this account, as its account manager's Upload Skin button. */
    public static boolean skinUploadEnabled(Account account) {
        if (!WawelAuthCompat.isLoaded() || account == null) return false;
        try {
            return WawelAuthBridge.skinUploadEnabled(account);
        } catch (LinkageError olderWawelAuth) {
            return true;
        }
    }

    /**
     * Uploads the skin, or stores it as the local skin of an offline account. Wawel Auth refreshes its caches itself.
     * The completion runs on the client thread.
     */
    public static void upload(Account account, BufferedImage image, boolean slim, Completion completion) {
        if (!WawelAuthCompat.isLoaded() || account == null || image == null) {
            completion.finished(null, I18n.format("salamander.upload.unavailable"));
            return;
        }
        try {
            WawelAuthBridge.uploadSkin(account, image, slim)
                .whenComplete((message, error) -> {
                    if (error == null) completion
                        .finished(message == null ? I18n.format("salamander.upload.completed") : message, null);
                    else completion.finished(null, usefulMessage(unwrap(error)));
                });
        } catch (LinkageError olderWawelAuth) {
            completion.finished(null, I18n.format("salamander.upload.unavailable"));
        }
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) current = current.getCause();
        return current;
    }

    private static String usefulMessage(Throwable error) {
        if (error == null) return I18n.format("salamander.upload.unknownError");
        String message = error.getMessage();
        return message == null || message.trim()
            .isEmpty() ? error.getClass()
                .getSimpleName() : message;
    }

    public interface Completion {

        void finished(String result, String error);
    }

    /** An account as Wawel Auth reports it. */
    public static final class Account {

        public final long id;
        public final String name;
        public final UUID uuid;
        public final String provider;
        public final boolean offline;

        public Account(long id, String name, UUID uuid, String provider, boolean offline) {
            this.id = id;
            this.name = name == null ? "?" : name;
            this.uuid = uuid;
            this.provider = provider == null ? "?" : provider;
            this.offline = offline;
        }
    }
}
