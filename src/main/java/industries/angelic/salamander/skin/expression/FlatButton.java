package industries.angelic.salamander.skin.expression;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

/**
 * The editor's small dark button: a tab, a toggle, a shape thumbnail, or a color swatch with its label. Selected ones
 * have a yellow border.
 */
final class FlatButton extends GuiButton {

    /** Draws a thumbnail inside the button. */
    interface Icon {

        void draw(int x, int y, int width, int height, boolean hovered);
    }

    boolean selected;
    /** A swatch's colors, drawn as stripes beside its label; null for other buttons. */
    int[] swatch;
    Icon icon;
    String tooltip;

    FlatButton(int id, int x, int y, int width, int height, String label) {
        super(id, x, y, width, height, label);
    }

    boolean hovered(int mouseX, int mouseY) {
        return this.visible && mouseX >= this.xPosition
            && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        if (!this.visible) return;
        boolean hovered = this.enabled && hovered(mouseX, mouseY);
        int x = this.xPosition, y = this.yPosition, right = x + this.width, bottom = y + this.height;
        int border = this.selected ? 0xffffff55 : hovered ? 0xffc0c0c0 : this.enabled ? 0xff606060 : 0xff383838;
        int fill = this.selected ? 0xff3a3a2a : hovered ? 0xff303030 : this.enabled ? 0xff1c1c1c : 0xff141414;
        Gui.drawRect(x, y, right, bottom, border);
        Gui.drawRect(x + 1, y + 1, right - 1, bottom - 1, fill);
        int text = !this.enabled ? 0xff707070 : this.selected ? 0xffffff55 : hovered ? 0xffffffa0 : 0xffe0e0e0;
        if (this.swatch != null) {
            int size = this.height - 4;
            Gui.drawRect(x + 2, y + 2, x + 2 + size, y + 2 + size, 0xff000000);
            if (this.swatch.length == 0) {
                Gui.drawRect(x + 3, y + 3, x + 1 + size, y + 1 + size, 0xff303030);
            } else {
                // Several colors show as stripes: the feature isn't one color.
                for (int i = 0; i < this.swatch.length; i++) {
                    int left = x + 3 + (size - 2) * i / this.swatch.length;
                    int stripeRight = x + 3 + (size - 2) * (i + 1) / this.swatch.length;
                    Gui.drawRect(left, y + 3, stripeRight, y + 1 + size, 0xff000000 | this.swatch[i]);
                }
            }
            minecraft.fontRenderer.drawString(
                minecraft.fontRenderer.trimStringToWidth(this.displayString, this.width - size - 7),
                x + size + 5,
                y + (this.height - 8) / 2,
                text);
            return;
        }
        if (this.icon != null) {
            this.icon.draw(x + 1, y + 1, this.width - 2, this.height - 2, hovered);
            return;
        }
        String label = minecraft.fontRenderer.trimStringToWidth(this.displayString, this.width - 4);
        minecraft.fontRenderer.drawString(
            label,
            x + (this.width - minecraft.fontRenderer.getStringWidth(label)) / 2,
            y + (this.height - 8) / 2,
            text);
    }
}
