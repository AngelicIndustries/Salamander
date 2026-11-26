package industries.angelic.salamander.api.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.skin.expression.ExpressionEditorScreen;

/**
 * Salamander's expression editor, which prepares a skin's eyes and brows for Just Expressions and Fresh Moves, for
 * other mods to open from their own screens. Client thread only.
 */
@SideOnly(Side.CLIENT)
public final class ExpressionEditor {

    private ExpressionEditor() {}

    /**
     * Opens the editor on the player's skin; Back returns to {@code parent}. Without a world there is no skin to edit,
     * and the editor says so.
     */
    public static void open(GuiScreen parent) {
        Minecraft.getMinecraft()
            .displayGuiScreen(new ExpressionEditorScreen(parent));
    }
}
