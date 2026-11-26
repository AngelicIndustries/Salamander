package industries.angelic.salamander.skin.expression;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**
 * Wawel Auth's Upload Skin button as its account manager draws it: its player icon on a dark square, light grey, white
 * while hovered.
 */
final class WawelAuthUploadButton extends GuiButton {

    private static final ResourceLocation ICON = new ResourceLocation("wawelauth", "textures/player_model_wide.png");
    /** Wawel Auth's button background and its icon tints. */
    private static final int IDLE = 0x78000000, HOVER = 0xFF000000;
    private static final float ICON_IDLE = 0xB8 / 255.0f, ICON_DIM = 0x68 / 255.0f;

    WawelAuthUploadButton(int id, int x, int y) {
        super(id, x, y, 20, 20, "");
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        if (!this.visible) return;
        boolean hovered = this.enabled && mouseX >= this.xPosition
            && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        Gui.drawRect(
            this.xPosition,
            this.yPosition,
            this.xPosition + this.width,
            this.yPosition + this.height,
            hovered ? HOVER : IDLE);
        minecraft.getTextureManager()
            .bindTexture(ICON);
        float tint = !this.enabled ? ICON_DIM : hovered ? 1.0f : ICON_IDLE;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(tint, tint, tint, 1.0f);
        // The icon is 16x32, drawn 8x16 as Wawel Auth draws it.
        Gui.func_152125_a(this.xPosition + (this.width - 8) / 2, this.yPosition + 2, 0, 0, 16, 32, 8, 16, 16, 32);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
