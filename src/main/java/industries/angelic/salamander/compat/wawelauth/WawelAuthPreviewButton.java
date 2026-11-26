package industries.angelic.salamander.compat.wawelauth;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.resources.I18n;

import industries.angelic.salamander.skin.expression.ExpressionEditorScreen;
import industries.angelic.salamander.skin.expression.FaceIcon;
import industries.angelic.wawelauth.api.account.AccountInfo;

/**
 * The expression editor's face button in Wawel Auth's account manager, after its skin and cape buttons. Wawel Auth's
 * button
 * interface is implemented by reflection, so Salamander builds against Wawel Auths that don't have it yet.
 */
final class WawelAuthPreviewButton implements InvocationHandler {

    private static final String API = "industries.angelic.wawelauth.api.gui.AccountPreviewButtons";
    /** The tints of Wawel Auth's skin and cape buttons. */
    private static final int IDLE_COLOR = 0xFFB8B8B8;
    private static final int HOVER_COLOR = 0xFFFFFFFF;

    private final FaceIcon face = new FaceIcon();

    private WawelAuthPreviewButton() {}

    /** Adds the button; throws when this Wawel Auth has no account manager buttons. */
    static void register() throws ReflectiveOperationException {
        ClassLoader loader = WawelAuthPreviewButton.class.getClassLoader();
        Class<?> buttons = Class.forName(API, true, loader);
        Class<?> button = Class.forName(API + "$PreviewButton", true, loader);
        Object proxy = Proxy.newProxyInstance(loader, new Class<?>[] { button }, new WawelAuthPreviewButton());
        buttons.getMethod("register", button)
            .invoke(null, proxy);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "drawIcon":
                this.drawIcon(
                    (Integer) args[0],
                    (Integer) args[1],
                    (Integer) args[2],
                    (Integer) args[3],
                    (Boolean) args[4]);
                return null;
            case "tooltip":
                return I18n.format("salamander.expression.accountButton");
            case "pressed":
                this.pressed((AccountInfo) args[0], (AbstractClientPlayer) args[1], (Runnable) args[2]);
                return null;
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return "Salamander's expression editor button";
            default:
                return null;
        }
    }

    /** The face fills the box Wawel Auth gives it: its menu's small icon square or a button's. */
    private void drawIcon(int x, int y, int width, int height, boolean hovered) {
        this.face.draw(x, y, width, height, hovered ? HOVER_COLOR : IDLE_COLOR, hovered);
    }

    private void pressed(AccountInfo account, AbstractClientPlayer preview, Runnable reopen) {
        WawelAuthUpload.Account target = new WawelAuthUpload.Account(
            account.id(),
            account.profileName(),
            account.profileUuid(),
            account.providerName(),
            account.offline());
        Minecraft.getMinecraft()
            .displayGuiScreen(ExpressionEditorScreen.forAccount(preview, target, reopen));
    }
}
