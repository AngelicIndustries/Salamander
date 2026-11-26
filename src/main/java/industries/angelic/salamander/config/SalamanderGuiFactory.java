package industries.angelic.salamander.config;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;

import cpw.mods.fml.client.config.DummyConfigElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.config.IConfigElement;
import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.skin.expression.ExpressionEditorScreen;
import industries.angelic.salamander.skin.expression.FaceIcon;

/**
 * Salamander's settings in the mod list: a button per category, the skin ones first, then, in a world, the expression
 * editor.
 */
public class SalamanderGuiFactory implements SimpleGuiFactory {

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigGui.class;
    }

    public static class ConfigGui extends SimpleGuiConfig {

        public ConfigGui(GuiScreen parent) throws ConfigException {
            super(
                parent,
                Salamander.MODID,
                Salamander.MODNAME,
                true,
                SkinConfig.class,
                SkinLayers3DConfig.class,
                SkullConfig.class,
                CemConfig.class,
                TextureConfig.class,
                DebugConfig.class);
            // The expression editor sits after the skin categories, as a button like theirs. It edits the player's
            // skin, so it shows only in a world.
            if (mc.thePlayer == null) return;
            configElements.add(
                Math.min(3, configElements.size()),
                new DummyConfigElement.DummyCategoryElement<>(
                    "expressionEditor",
                    "salamander.config.expressioneditor",
                    ExpressionEditorEntry.class));
            entryList = new GuiConfigEntries(this, mc);
            initEntries.clear();
            initEntries.addAll(entryList.listEntries);
        }
    }

    /**
     * A category button that opens the expression editor instead of a page of settings, with its face left of the
     * label.
     */
    public static class ExpressionEditorEntry extends GuiConfigEntries.CategoryEntry {

        /** The face's box, as Wawel Auth's menu gives its icons, and its gap to the label. */
        private static final int FACE_SIZE = 12;
        private static final int FACE_GAP = 4;

        private final FaceIcon face = new FaceIcon();

        public ExpressionEditorEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList,
            IConfigElement<?> configElement) {
            super(owningScreen, owningEntryList, configElement);
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator,
            int mouseX, int mouseY, boolean isSelected) {
            super.drawEntry(slotIndex, x, y, listWidth, slotHeight, tessellator, mouseX, mouseY, isSelected);
            int size = FACE_SIZE;
            int label = this.mc.fontRenderer.getStringWidth(this.btnSelectCategory.displayString);
            boolean hovered = this.btnSelectCategory.enabled && this.btnSelectCategory.func_146115_a();
            // The label's colors, as the button draws it.
            int color = !this.btnSelectCategory.enabled ? 0xFFA0A0A0 : hovered ? 0xFFFFFFA0 : 0xFFE0E0E0;
            this.face.draw(
                this.btnSelectCategory.xPosition + this.btnSelectCategory.width / 2 - label / 2 - FACE_GAP - size,
                this.btnSelectCategory.yPosition + (this.btnSelectCategory.height - size) / 2.0f,
                size,
                size,
                color,
                hovered);
        }

        @Override
        protected GuiScreen buildChildScreen() {
            return new ExpressionEditorScreen(this.owningScreen);
        }
    }
}
