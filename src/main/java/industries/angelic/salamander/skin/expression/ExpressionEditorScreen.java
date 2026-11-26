package industries.angelic.salamander.skin.expression;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import industries.angelic.salamander.api.skin.PlayerSkins;
import industries.angelic.salamander.compat.wawelauth.WawelAuthCompat;
import industries.angelic.salamander.compat.wawelauth.WawelAuthUpload;
import industries.angelic.salamander.compat.wawelauth.WawelAuthUpload.Account;
import industries.angelic.salamander.skin.expression.ExpressionDraft.Brows;
import industries.angelic.salamander.skin.expression.ExpressionDraft.Fix;
import industries.angelic.salamander.skin.expression.ExpressionDraft.Pick;
import industries.angelic.salamander.skin.expression.ExpressionDraft.Problem;
import industries.angelic.salamander.skin.expression.ExpressionDraft.Swatch;
import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EditorData;
import industries.angelic.salamander.skin.expression.ExpressionSkinExporter.EyeType;

/**
 * Prepares a skin for Just Expressions or Fresh Moves, whose eyes and brows Salamander draws through their pack models.
 * In a world it edits the player's skin; from Wawel Auth's account manager, the selected account's.
 * <p>
 * One page: the face to place features on, tabs for the eyes, the brows and the extras, a 3D head that moves as the
 * packs will, and what the packs can't show yet, with fixes. A new skin opens with a draft already found in it; a
 * prepared one opens as it was.
 */
public class ExpressionEditorScreen extends GuiScreen {

    private enum Tab {
        EYES,
        BROWS,
        EXTRAS
    }

    private static final int BACK = 1, SAVE = 2, UPLOAD = 3, TAB = 10, SHAPE = 20, EYE_UP = 30, EYE_DOWN = 31,
        MIRROR = 32, PICK = 33, DETECT = 34, WHITE = 35, PUPIL = 36, LID = 37, PAINT_LIDS = 38, MOVING_BROWS = 40,
        BROW_SHAPE = 41, BROW_UP = 44, BROW_DOWN = 45, BROW_MIRROR = 46, BROW = 47, UNDER_BROW = 48, PAINT_BROWS = 49,
        OUTER = 50, OUTER_UP = 51, OUTER_DOWN = 52, OUTER_COLOR = 53, LASHES = 54, LASH_TOP = 55, LASH_SIDE = 56,
        BLINK = 60, LOOK = 61, SURPRISED = 62, POPUP_DONE = 70, FIX = 100;
    private static final int PICKER_WIDTH = 104, PICKER_HEIGHT = 36, HUE_WIDTH = 10;
    /** Degrees the head turns per pixel dragged; how far it turns toward the pointer; how fast both ease. */
    private static final float TURN_PER_PIXEL = 1.5f, LOOK_YAW = 12.0f, LOOK_PITCH = 6.0f, TURN_EASE_SECONDS = 0.05f,
        LOOK_EASE_SECONDS = 0.15f;
    private static final long BLINK_MS = 300L, AUTO_BLINK_MS = 4000L, LOOK_STEP_MS = 700L, SURPRISE_MS = 1600L;

    /** The draft as it was before an edit, for undo. */
    private static final class Snapshot {

        final EditorData data;
        final BufferedImage skin;
        final Boolean[] outer;
        final boolean[][] edited;

        Snapshot(EditorData data, BufferedImage skin, Boolean[] outer, boolean[][] edited) {
            this.data = data;
            this.skin = skin;
            this.outer = outer.clone();
            this.edited = new boolean[edited.length][];
            for (int i = 0; i < edited.length; i++) this.edited[i] = edited[i].clone();
        }
    }

    private final Runnable back;
    /** The account whose skin this is, where uploads go; null for the player's, uploaded to the active account. */
    private final Account account;
    private final EditorData data = new EditorData();
    /** Pupils picked by hand per side: true outer, false inner, null as the skin has them. */
    private Boolean[] outer = new Boolean[2];
    /** Swatches whose color was chosen by hand, per side: placing a feature again keeps them. */
    private boolean[][] edited = new boolean[Swatch.values().length][2];
    private final Deque<Snapshot> undo = new ArrayDeque<>();
    private final Deque<Snapshot> redo = new ArrayDeque<>();
    private final ExpressionHead head = new ExpressionHead();
    private final ExpressionPreview preview = new ExpressionPreview();
    /** The player Wawel Auth previewed the account with, whose world a preview can stand in at the main menu. */
    private AbstractClientPlayer previewPlayer;
    /** The skin the pack preview wears: the draft as saved, or the skin as it is while the draft has problems. */
    private BufferedImage previewSkin;
    /** Whether the pack draws the preview, so the moves are its own; checked as the preview draws. */
    private boolean packPreview;
    private BufferedImage skin;
    private boolean slim;
    private boolean skinRead;
    private boolean started;
    private boolean uploading;
    private String name;

    private Tab tab = Tab.EYES;
    private boolean mirror = true;
    private int side;
    private boolean pickMode;
    private boolean paintLids;
    private boolean paintBrows;
    private int lidBrush = 0xff000000;
    private int browBrush = 0xff000000;
    /** The swatch whose color picker is open, or null; brush when it sets the advanced painting color. */
    private Swatch popup;
    private boolean popupBrush;
    private float hue, saturation, brightness = 1.0f;
    private GuiTextField hexField;
    private int pickerDrag;

    private String status = "";
    private boolean statusGood;
    private String note;
    private List<Problem> problems = Collections.emptyList();
    private final List<String[]> infoLines = new ArrayList<>();

    private int dragX = -1, dragY, dragToX, dragToY;
    private boolean painting, paintValue, turning;
    /** Where a drag on the head started, and the turn then. */
    private int pressX, pressY;
    private float pressYaw, pressPitch;
    /** The head's turn: where dragging puts it, and as drawn, easing toward that; positive yaw turns its face right. */
    private float yaw = -8.0f, pitch = 0.0f, shownYaw = -8.0f, shownPitch = 0.0f;
    /** How far the head turns toward the pointer, as drawn, easing toward where the pointer is. */
    private float lookYaw, lookPitch;
    private long lastFrameNanos;
    private long blinkAt = -1L << 40, lookAt = -1L << 40, surprisedAt = -1L << 40;
    private int lastMouseX, lastMouseY;

    // Layout
    private boolean wide;
    private int px, canvasX, canvasY, controlsX, controlsY, headX, headY, headSize, infoX, infoY, infoWidth, infoBottom;

    /** Edits the player's skin once it opens; Back returns to parent. */
    public ExpressionEditorScreen(GuiScreen parent) {
        this(
            () -> Minecraft.getMinecraft()
                .displayGuiScreen(parent),
            null);
    }

    private ExpressionEditorScreen(Runnable back, Account account) {
        this.back = back;
        this.account = account;
    }

    /**
     * Edits an account's skin as its preview player draws it: exports are named after the account, uploads go to it,
     * and Back runs back.
     */
    public static ExpressionEditorScreen forAccount(AbstractClientPlayer preview, Account account, Runnable back) {
        ExpressionEditorScreen screen = new ExpressionEditorScreen(back, account);
        // Read now: the preview's skin may be released once its screen closes.
        screen.readSkin(preview);
        screen.previewPlayer = preview;
        return screen;
    }

    private void readSkin(AbstractClientPlayer player) {
        this.skinRead = true;
        this.skin = EditorSkin.of(player);
        this.slim = player != null && PlayerSkins.slim(player);
    }

    // Opening

    private void start() {
        this.started = true;
        if (!this.skinRead) readSkin(this.mc.thePlayer);
        this.name = this.account != null ? this.account.name
            : this.mc.thePlayer != null ? this.mc.thePlayer.getCommandSenderName()
                : this.mc.getSession()
                    .getUsername();
        if (this.skin == null) {
            setStatus(I18n.format("salamander.expression.noSkin"), false);
            return;
        }
        if (!ExpressionDraft.usable(this.skin)) return;
        EditorData loaded = ExpressionSkinExporter.readEditorData(this.skin);
        if (loaded != null) {
            this.data.copyFrom(loaded);
            for (boolean[] swatch : this.edited) swatch[0] = swatch[1] = true;
            setStatus(I18n.format("salamander.expression.loaded"), true);
        } else {
            detect();
        }
    }

    /** Replaces the draft with what is found in the skin. */
    private void detect() {
        ExpressionDetector.Result found = ExpressionDetector.detect(this.skin);
        this.data.copyFrom(found.data);
        this.outer = new Boolean[2];
        this.edited = new boolean[Swatch.values().length][2];
        if (!found.eyesFound) {
            setStatus(I18n.format("salamander.expression.notFound"), false);
            return;
        }
        String pupils = found.eyes.width == 1 ? I18n.format("salamander.expression.pupils.none")
            : !ExpressionDraft.pupilSideFree(found.eyes) ? I18n.format("salamander.expression.pupils.inside")
                : ExpressionDraft.outerPupil(this.data, 0) || ExpressionDraft.outerPupil(this.data, 1)
                    ? I18n.format("salamander.expression.pupils.outside")
                    : I18n.format("salamander.expression.pupils.inside");
        String text = I18n.format("salamander.expression.found", shapeName(found.eyes), found.eyeRow, pupils);
        if (found.brows != null) text += " " + I18n.format("salamander.expression.found.brows", found.browRow);
        setStatus(text, true);
    }

    private void setStatus(String text, boolean good) {
        this.status = text;
        this.statusGood = good;
    }

    @Override
    public void initGui() {
        if (!this.started) start();
        layout();
        this.buttonList.clear();
        int x = this.controlsX, y = this.controlsY;
        String[] tabs = { "eyes", "brows", "extras" };
        for (int i = 0; i < 3; i++) this.buttonList
            .add(flat(TAB + i, x + i * 62, y, 58, 14, I18n.format("salamander.expression.tab." + tabs[i])));
        // Eyes
        for (int i = 0; i < ExpressionDraft.SHAPES.length; i++) {
            EyeType type = ExpressionDraft.SHAPES[i];
            FlatButton button = flat(SHAPE + i, x + i * 20, y + 28, 18, 18, "");
            button.icon = (ix, iy, w, h, hovered) -> FaceIcon
                .drawShape(ix + (w - 16) / 2.0f, iy + (h - 16) / 2.0f, 2, hovered ? 0xffffffff : 0xffc8c8c8, type);
            button.tooltip = shapeName(type);
            this.buttonList.add(button);
        }
        this.buttonList.add(flat(EYE_UP, x + 44, y + 50, 28, 14, I18n.format("salamander.expression.up")));
        this.buttonList.add(flat(EYE_DOWN, x + 74, y + 50, 28, 14, I18n.format("salamander.expression.down")));
        this.buttonList.add(flat(MIRROR, x + 106, y + 50, 76, 14, ""));
        this.buttonList.add(flat(PICK, x, y + 68, 100, 14, ""));
        this.buttonList.add(flat(DETECT, x + 104, y + 68, 78, 14, I18n.format("salamander.expression.detect")));
        this.buttonList.add(swatch(WHITE, x, y + 86, "white"));
        this.buttonList.add(swatch(PUPIL, x + 62, y + 86, "pupil"));
        this.buttonList.add(swatch(LID, x + 124, y + 86, "lid"));
        this.buttonList.add(flat(PAINT_LIDS, x, y + 104, 182, 14, ""));
        // Brows
        this.buttonList.add(flat(MOVING_BROWS, x, y + 18, 182, 14, ""));
        for (int i = 0; i < 3; i++) {
            Brows kind = Brows.values()[i];
            FlatButton button = flat(BROW_SHAPE + i, x + 44 + i * 28, y + 36, 26, 18, "");
            button.icon = (ix, iy, w, h, hovered) -> FaceIcon
                .drawBrows(ix + (w - 16) / 2.0f, iy + (h - 16) / 2.0f, 2, hovered ? 0xffffffff : 0xffc8c8c8, kind);
            button.tooltip = I18n.format("salamander.expression.brow." + i);
            this.buttonList.add(button);
        }
        this.buttonList.add(flat(BROW_UP, x + 44, y + 58, 28, 14, I18n.format("salamander.expression.up")));
        this.buttonList.add(flat(BROW_DOWN, x + 74, y + 58, 28, 14, I18n.format("salamander.expression.down")));
        this.buttonList.add(flat(BROW_MIRROR, x + 106, y + 58, 76, 14, ""));
        this.buttonList.add(swatch(BROW, x, y + 76, "brow"));
        FlatButton under = swatch(UNDER_BROW, x + 62, y + 76, "under_brow");
        under.width = 90;
        this.buttonList.add(under);
        this.buttonList.add(flat(PAINT_BROWS, x, y + 94, 182, 14, ""));
        // Extras
        this.buttonList.add(flat(OUTER, x, y + 18, 120, 14, ""));
        this.buttonList.add(flat(OUTER_UP, x + 124, y + 18, 28, 14, I18n.format("salamander.expression.up")));
        this.buttonList.add(flat(OUTER_DOWN, x + 154, y + 18, 28, 14, I18n.format("salamander.expression.down")));
        this.buttonList.add(swatch(OUTER_COLOR, x, y + 36, "outer"));
        this.buttonList.add(flat(LASHES, x, y + 58, 120, 14, ""));
        this.buttonList.add(swatch(LASH_TOP, x, y + 76, "lash_top"));
        this.buttonList.add(swatch(LASH_SIDE, x + 62, y + 76, "lash_side"));
        // Preview
        String[] moves = { "blink", "look", "surprised" };
        for (int i = 0; i < 3; i++) {
            int bx = this.wide ? this.headX : this.canvasX;
            int by = this.wide ? this.headY + this.headSize + 4 + i * 16 : this.headY + this.headSize + 2 + i * 14;
            int bw = this.wide ? this.headSize : this.px * 8;
            this.buttonList.add(flat(BLINK + i, bx, by, bw, 14, I18n.format("salamander.expression.move." + moves[i])));
        }
        this.buttonList.add(flat(POPUP_DONE, x + 128, y + 102, 50, 14, I18n.format("gui.done")));
        // Main buttons
        int bottom = this.height - 26;
        int backWidth = this.wide ? 80 : 60, saveWidth = this.wide ? 110 : 96;
        boolean wawelAuth = WawelAuthCompat.isLoaded();
        int total = backWidth + 4 + saveWidth + (wawelAuth ? 4 + 20 : 0);
        int bx = (this.width - total) / 2;
        this.buttonList.add(new GuiButton(BACK, bx, bottom, backWidth, 20, I18n.format("gui.back")));
        this.buttonList.add(
            new GuiButton(SAVE, bx + backWidth + 4, bottom, saveWidth, 20, I18n.format("salamander.expression.save")));
        if (wawelAuth) this.buttonList.add(new WawelAuthUploadButton(UPLOAD, bx + backWidth + saveWidth + 8, bottom));
        this.hexField = new GuiTextField(this.fontRendererObj, x + 40, y + 76, 64, 14);
        this.hexField.setMaxStringLength(7);
        Keyboard.enableRepeatEvents(true);
        refresh();
    }

    private void layout() {
        this.wide = this.width >= 380;
        this.px = this.wide ? 16 : 12;
        int canvas = this.px * 8;
        int content = this.wide ? canvas + 8 + 182 + 8 + 84 : canvas + 8 + 182;
        int left = (this.width - content) / 2;
        int top = 20 + Math.max(0, (this.height - 240) / 4);
        this.canvasX = left;
        this.canvasY = top;
        this.controlsX = left + canvas + 8;
        this.controlsY = top;
        if (this.wide) {
            this.headX = this.controlsX + 190;
            this.headY = top;
            this.headSize = 84;
            this.infoX = left;
            this.infoY = top + canvas + 4;
            this.infoWidth = canvas + 8 + 182;
        } else {
            // Narrow: the head and its moves under the face, the text under the controls.
            this.headSize = 48;
            this.headX = left + (canvas - this.headSize) / 2;
            this.headY = top + canvas + 4;
            this.infoX = this.controlsX;
            this.infoY = top + 124;
            this.infoWidth = 182;
        }
        this.infoBottom = this.height - 30;
        if (this.skin != null && !ExpressionDraft.usable(this.skin)) {
            // Nothing to edit: the message takes the page's width, where the face would be.
            this.infoX = left;
            this.infoY = top + 4;
            this.infoWidth = content;
        }
    }

    private FlatButton flat(int id, int x, int y, int width, int height, String label) {
        return new FlatButton(id, x, y, width, height, label);
    }

    private FlatButton swatch(int id, int x, int y, String name) {
        FlatButton button = new FlatButton(id, x, y, 58, 14, I18n.format("salamander.expression.swatch." + name));
        button.swatch = new int[0];
        return button;
    }

    private Account uploadTarget() {
        return this.account != null ? this.account : WawelAuthUpload.activeAccount();
    }

    // State to controls

    /** Brings buttons, labels and the problems in line with the draft and the open tab. */
    private void refresh() {
        boolean popupOpen = this.popup != null;
        boolean usable = ExpressionDraft.usable(this.skin);
        this.problems = ExpressionDraft.problems(this.skin, this.data);
        this.previewSkin = previewSkin();
        EyeType shape = ExpressionDraft.shape(this.data, this.side);
        Brows brows = ExpressionDraft.brows(this.data);
        int[] sides = sides(this.side);
        boolean lashesFit = true;
        for (int s : sides) lashesFit &= ExpressionDraft.lashesFit(this.data, s);
        for (Object object : this.buttonList) {
            GuiButton button = (GuiButton) object;
            int id = button.id;
            FlatButton flat = button instanceof FlatButton ? (FlatButton) button : null;
            if (id >= TAB && id < TAB + 3) {
                flat.selected = this.tab.ordinal() == id - TAB;
                button.visible = !popupOpen;
            } else if (id >= SHAPE && id < SHAPE + ExpressionDraft.SHAPES.length) {
                button.visible = this.tab == Tab.EYES && !popupOpen;
                flat.selected = ExpressionDraft.SHAPES[id - SHAPE] == shape;
            } else if (id >= EYE_UP && id <= PAINT_LIDS) {
                button.visible = this.tab == Tab.EYES && !popupOpen;
            } else if (id >= MOVING_BROWS && id <= PAINT_BROWS) {
                button.visible = this.tab == Tab.BROWS && !popupOpen;
            } else if (id >= OUTER && id <= LASH_SIDE) {
                button.visible = this.tab == Tab.EXTRAS && !popupOpen;
            } else if (id == POPUP_DONE) {
                button.visible = popupOpen;
            }
            if (id != BACK) button.enabled = usable && !this.uploading;
            // Nothing to edit in a skin the packs can't read: only Back and the message under it remain.
            if (!usable && id != BACK) button.visible = false;
            if (id >= BLINK && id <= SURPRISED) button.visible &= !this.packPreview;
            switch (id) {
                case MIRROR:
                case BROW_MIRROR:
                    button.displayString = I18n.format("salamander.expression.mirror", onOff(this.mirror));
                    flat.selected = this.mirror;
                    flat.tooltip = I18n.format("salamander.expression.mirror.tooltip");
                    break;
                case PICK:
                    button.displayString = I18n.format("salamander.expression.pick", onOff(this.pickMode));
                    flat.selected = this.pickMode;
                    flat.tooltip = I18n.format("salamander.expression.pick.tooltip");
                    break;
                case PAINT_LIDS:
                    button.displayString = I18n.format("salamander.expression.paintLids", onOff(this.paintLids));
                    flat.selected = this.paintLids;
                    break;
                case PAINT_BROWS:
                    button.displayString = I18n.format("salamander.expression.paintBrows", onOff(this.paintBrows));
                    flat.selected = this.paintBrows;
                    button.enabled &= brows != null;
                    break;
                case EYE_UP:
                case EYE_DOWN:
                    button.enabled &= shape != null;
                    break;
                case MOVING_BROWS:
                    button.displayString = I18n.format("salamander.expression.movingBrows", onOff(brows != null));
                    flat.selected = brows != null;
                    flat.tooltip = I18n.format("salamander.expression.movingBrows.tooltip");
                    break;
                case BROW_UP:
                case BROW_DOWN:
                    button.enabled &= brows == Brows.PAIRED;
                    flat.tooltip = brows == Brows.PAIRED ? null : I18n.format("salamander.expression.browRow3");
                    break;
                case OUTER: {
                    boolean on = ExpressionDraft.hasOuterBrows(this.data, this.side);
                    button.displayString = I18n.format("salamander.expression.outerBrows", onOff(on));
                    flat.selected = on;
                    flat.tooltip = I18n.format("salamander.expression.outerBrows.tooltip");
                    break;
                }
                case OUTER_UP:
                case OUTER_DOWN:
                case OUTER_COLOR:
                    button.enabled &= ExpressionDraft.hasOuterBrows(this.data, this.side);
                    break;
                case LASHES: {
                    boolean on = ExpressionDraft.hasLashes(this.data, this.side);
                    button.displayString = I18n.format("salamander.expression.lashes", onOff(on));
                    flat.selected = on;
                    button.enabled &= on || lashesFit;
                    flat.tooltip = lashesFit ? null : I18n.format("salamander.expression.lashes.unavailable");
                    break;
                }
                case LASH_TOP:
                case LASH_SIDE:
                    button.enabled &= ExpressionDraft.hasLashes(this.data, this.side);
                    break;
                case SAVE:
                case UPLOAD:
                    button.enabled &= blocking().isEmpty();
                    // As in Wawel Auth's account manager: only where the provider takes skin uploads.
                    if (id == UPLOAD)
                        button.visible = uploadTarget() != null && WawelAuthUpload.skinUploadEnabled(uploadTarget());
                    break;
                default:
                    break;
            }
            Swatch swatch = swatchOf(id);
            if (swatch != null) {
                flat.swatch = ExpressionDraft.colors(this.data, swatch, this.side);
                boolean brush = swatch == Swatch.LID && this.paintLids || swatch == Swatch.BROW && this.paintBrows;
                if (brush) flat.swatch = new int[] { swatch == Swatch.LID ? this.lidBrush : this.browBrush };
                flat.selected = this.popup == swatch;
                flat.tooltip = I18n.format(
                    brush ? "salamander.expression.swatch.brush.tooltip" : "salamander.expression.swatch.tooltip");
                if (!brush) button.enabled &= ExpressionDraft.pixels(this.data, swatch, this.side)
                    .size() > 0;
            }
        }
        this.hexField.setVisible(popupOpen);
        if (!popupOpen) this.hexField.setFocused(false);
        layoutInfo();
    }

    /** The draft as it would be saved, or while it can't be, the skin as it is, both at 64x64; null without a skin. */
    private BufferedImage previewSkin() {
        if (!ExpressionDraft.usable(this.skin)) return null;
        if (this.problems.isEmpty()) {
            try {
                return ExpressionSkinExporter.export(this.skin, this.data);
            } catch (IllegalArgumentException unexpected) {
                // Shown in the problems list.
            }
        }
        return industries.angelic.salamander.cem.client.CemSkinImage.normalize(this.skin);
    }

    /**
     * The lines under the face: the hint, the last result, notes, then each problem with its fix button (beside it, or
     * under it in narrow windows). Problems come before the hint when there isn't room for all.
     */
    private void layoutInfo() {
        this.buttonList.removeIf(object -> ((GuiButton) object).id >= FIX);
        this.infoLines.clear();
        List<String[]> lines = new ArrayList<>();
        // Fix buttons as problem index, the text row they go on, and label.
        List<Object[]> fixes = new ArrayList<>();
        List<String[]> results = new ArrayList<>();
        if (!this.status.isEmpty())
            addLines(results, this.status, this.statusGood ? "8fdf8f" : "ffaa55", this.infoWidth);
        if (this.note != null) addLines(results, this.note, "ffff88", this.infoWidth);
        for (String held : heldNotes()) addLines(results, held, "ffff88", this.infoWidth);
        if (!this.packPreview && ExpressionDraft.usable(this.skin)) addLines(
            results,
            I18n.format(
                ExpressionPreview.world(this.previewPlayer) != null ? "salamander.expression.needPack"
                    : "salamander.expression.noWorld"),
            "a0a0a0",
            this.infoWidth);
        int fixWidth = this.wide ? 112 : 120;
        int rows = (this.infoBottom - this.infoY) / 10;
        int shown = 0;
        for (int index = 0; index < this.problems.size(); index++) {
            Problem problem = this.problems.get(index);
            String text = problem.key == null ? problem.detail
                : I18n.format("salamander.expression.problem." + problem.key);
            boolean fixable = problem.fix != Fix.NONE;
            List<String[]> block = new ArrayList<>();
            addLines(block, text, "ff7755", fixable && this.wide ? this.infoWidth - fixWidth - 4 : this.infoWidth);
            int fixRow = this.wide ? 0 : block.size();
            if (fixable && !this.wide) {
                block.add(new String[] { "", "ffffff" });
                block.add(new String[] { "", "ffffff" });
            }
            // A problem shows whole or not at all; a line says how many more wait below.
            boolean last = index == this.problems.size() - 1;
            if (lines.size() + block.size() + (last ? 0 : 1) > rows && shown > 0) {
                addLines(
                    lines,
                    I18n.format("salamander.expression.moreProblems", this.problems.size() - index),
                    "ff7755",
                    this.infoWidth);
                break;
            }
            if (fixable) fixes.add(
                new Object[] { index, lines.size() + fixRow,
                    I18n.format(
                        "salamander.expression.fix." + problem.fix.name()
                            .toLowerCase(java.util.Locale.ROOT)) });
            lines.addAll(block);
            shown++;
        }
        if (this.problems.isEmpty() && ExpressionDraft.usable(this.skin))
            addLines(lines, I18n.format("salamander.expression.ready"), "55ff55", this.infoWidth);
        // Problems come first: results go above them while they fit, then the hint. Narrow windows show the hint on
        // the face's hover instead.
        int offset = 0;
        if (lines.size() + results.size() <= rows) {
            lines.addAll(0, results);
            offset = results.size();
        }
        List<String[]> hint = new ArrayList<>();
        if (ExpressionDraft.usable(this.skin)) addLines(hint, hint(), "bfbfbf", this.infoWidth);
        if (this.wide && lines.size() + hint.size() <= rows) {
            lines.addAll(0, hint);
            offset += hint.size();
        }
        for (int row = 0; row < lines.size(); row++) {
            int y = this.infoY + row * 10;
            if (y + 9 > this.infoBottom) break;
            this.infoLines.add(new String[] { lines.get(row)[0], lines.get(row)[1], String.valueOf(y) });
        }
        for (Object[] fix : fixes) {
            int y = this.infoY + ((Integer) fix[1] + offset) * 10 - 2;
            if (y + 12 > this.infoBottom) continue;
            FlatButton button = flat(
                FIX + (Integer) fix[0],
                this.wide ? this.infoX + this.infoWidth - fixWidth : this.infoX,
                y,
                fixWidth,
                12,
                (String) fix[2]);
            button.enabled = !this.uploading;
            this.buttonList.add(button);
        }
    }

    private void addLines(List<String[]> lines, String text, String color, int width) {
        for (Object line : this.fontRendererObj.listFormattedStringToWidth(text, width))
            lines.add(new String[] { (String) line, color });
    }

    /** Notes that hold while the draft is so: hand picks a shape can't keep. */
    private List<String> heldNotes() {
        List<String> notes = new ArrayList<>();
        for (int s = 0; s < 2; s++) {
            EyeType shape = ExpressionDraft.shape(this.data, s);
            if (Boolean.TRUE.equals(this.outer[s]) && shape != null && !ExpressionDraft.pupilSideFree(shape)) {
                notes.add(I18n.format("salamander.expression.note.outerKept", shapeName(shape)));
                break;
            }
        }
        return notes;
    }

    private String hint() {
        if (this.popup != null) return I18n.format("salamander.expression.hint.popup");
        switch (this.tab) {
            case EYES:
                return I18n.format(
                    this.pickMode ? "salamander.expression.hint.pick"
                        : this.paintLids ? "salamander.expression.hint.lids" : "salamander.expression.hint.eyes");
            case BROWS:
                return I18n.format(
                    this.paintBrows ? "salamander.expression.hint.paintBrows" : "salamander.expression.hint.brows");
            default:
                return I18n.format("salamander.expression.hint.extras");
        }
    }

    private List<Problem> blocking() {
        return this.problems;
    }

    private static String onOff(boolean on) {
        return I18n.format(on ? "salamander.expression.on" : "salamander.expression.off");
    }

    private static Swatch swatchOf(int id) {
        switch (id) {
            case WHITE:
                return Swatch.WHITE;
            case PUPIL:
                return Swatch.PUPIL;
            case LID:
                return Swatch.LID;
            case BROW:
                return Swatch.BROW;
            case UNDER_BROW:
                return Swatch.UNDER_BROW;
            case OUTER_COLOR:
                return Swatch.OUTER;
            case LASH_TOP:
                return Swatch.LASH_TOP;
            case LASH_SIDE:
                return Swatch.LASH_SIDE;
            default:
                return null;
        }
    }

    private static String shapeName(EyeType type) {
        return I18n.format(
            "salamander.expression.eye." + type.name()
                .toLowerCase(java.util.Locale.ROOT));
    }

    private int[] sides(int side) {
        return this.mirror ? new int[] { 0, 1 } : new int[] { side };
    }

    // Edits

    private void remember() {
        this.undo.push(new Snapshot(this.data.copy(), this.skin, this.outer, this.edited));
        while (this.undo.size() > 60) this.undo.removeLast();
        this.redo.clear();
    }

    private void undo(boolean forward) {
        Deque<Snapshot> from = forward ? this.redo : this.undo;
        Deque<Snapshot> to = forward ? this.undo : this.redo;
        if (from.isEmpty()) return;
        to.push(new Snapshot(this.data.copy(), this.skin, this.outer, this.edited));
        Snapshot snapshot = from.pop();
        this.data.copyFrom(snapshot.data);
        this.skin = snapshot.skin;
        this.outer = snapshot.outer.clone();
        this.edited = snapshot.edited;
        this.note = null;
        refresh();
    }

    /** Places eyes of this shape on the side's eyes (or both, mirrored), keeping colors chosen by hand. */
    private void placeEyes(EyeType type, int row, int side) {
        for (int s : sides(side)) {
            int[] kept = keptColors(s, Swatch.WHITE, Swatch.PUPIL, Swatch.LID);
            ExpressionDraft.placeEyes(this.data, this.skin, type, row, s, this.outer[s]);
            // Lashes that no longer fit stay, for the problems list to offer removing them.
            restoreColors(s, kept, Swatch.WHITE, Swatch.PUPIL, Swatch.LID);
        }
    }

    private int[] keptColors(int side, Swatch... swatches) {
        int[] kept = new int[swatches.length];
        for (int i = 0; i < swatches.length; i++) {
            int[] colors = ExpressionDraft.colors(this.data, swatches[i], side);
            kept[i] = this.edited[swatches[i].ordinal()][side] && colors.length > 0 ? colors[0] : 0;
        }
        return kept;
    }

    private void restoreColors(int side, int[] kept, Swatch... swatches) {
        for (int i = 0; i < swatches.length; i++)
            if ((kept[i] >>> 24) != 0) ExpressionDraft.paint(this.data, swatches[i], side, kept[i]);
    }

    private void moveEyes(int step) {
        EyeType shape = ExpressionDraft.shape(this.data, this.side);
        if (shape == null) return;
        int row = ExpressionDraft.eyeRow(this.data, this.side) + step;
        remember();
        this.note = null;
        placeEyes(shape, Math.max(shape.minRow, Math.min(shape.maxRow, row)), this.side);
        if (row < shape.minRow || row > shape.maxRow)
            this.note = I18n.format("salamander.expression.note.rows", shapeName(shape), shape.minRow, shape.maxRow);
    }

    private void setShape(EyeType type) {
        remember();
        this.note = null;
        int row = ExpressionDraft.eyeRow(this.data, this.side);
        if (row < 0) row = 4;
        placeEyes(type, Math.max(type.minRow, Math.min(type.maxRow, row)), this.side);
    }

    private void pickPupil(int x, int y) {
        Boolean[] picked = this.outer.clone();
        Pick result = ExpressionDraft.pick(this.data, x, y, picked);
        int s = x < 4 ? 0 : 1;
        if (result != Pick.DONE) {
            this.note = I18n.format(
                "salamander.expression.pick." + result.name()
                    .toLowerCase(java.util.Locale.ROOT));
            return;
        }
        remember();
        EyeType shape = ExpressionDraft.shape(this.data, s);
        int row = ExpressionDraft.eyeRow(this.data, s);
        for (int other : sides(s)) {
            this.outer[other] = picked[s];
            int[] kept = keptColors(other, Swatch.WHITE, Swatch.PUPIL, Swatch.LID);
            ExpressionDraft.placeEyes(this.data, this.skin, shape, row, other, this.outer[other]);
            restoreColors(other, kept, Swatch.WHITE, Swatch.PUPIL, Swatch.LID);
        }
        this.note = I18n.format(picked[s] ? "salamander.expression.pick.outer" : "salamander.expression.pick.inner");
    }

    private void setBrows(Brows kind, int row) {
        remember();
        int[][] kept = new int[2][];
        for (int s = 0; s < 2; s++) kept[s] = keptColors(s, Swatch.BROW, Swatch.UNDER_BROW);
        boolean replaceAll = kind != Brows.PAIRED || this.mirror;
        if (replaceAll) ExpressionDraft.placeBrows(this.data, this.skin, kind, row, -1);
        else ExpressionDraft.placeBrows(this.data, this.skin, kind, row, this.side);
        for (int s = 0; s < 2; s++) restoreColors(s, kept[s], Swatch.BROW, Swatch.UNDER_BROW);
    }

    private int browRowOrDefault() {
        int row = ExpressionDraft.browRow(this.data, this.side);
        if (row >= 0) return row;
        int eyes = ExpressionDraft.eyeRow(this.data, this.side);
        return Math.max(2, Math.min(5, eyes < 0 ? 3 : eyes - 1));
    }

    private void setOuterBrows(boolean on, int row) {
        remember();
        for (int s : sides(this.side)) {
            if (on) ExpressionDraft.placeOuterBrows(this.data, this.skin, row, s);
            else ExpressionDraft.removeOuterBrows(this.data, s);
        }
    }

    private void applyFix(Problem problem) {
        remember();
        if (problem.fix == Fix.PLACE_EYES_AGAIN) {
            for (int s = 0; s < 2; s++) if (ExpressionDraft.hasEyes(this.data, s)) {
                ExpressionDraft.removeEyes(this.data, s);
                ExpressionDraft.placeEyes(this.data, this.skin, EyeType.NORMAL_2X1, 4, s, this.outer[s]);
            }
        } else {
            BufferedImage fixed = ExpressionDraft.fix(problem.fix, this.skin, this.data);
            if (fixed != this.skin) {
                this.skin = fixed;
                // The face changed under the eyes: colors nobody chose take the new pixels.
                for (int s = 0; s < 2; s++) {
                    EyeType shape = ExpressionDraft.shape(this.data, s);
                    if (shape == null) continue;
                    int[] kept = keptColors(s, Swatch.WHITE, Swatch.PUPIL, Swatch.LID);
                    ExpressionDraft
                        .placeEyes(this.data, this.skin, shape, ExpressionDraft.eyeRow(this.data, s), s, this.outer[s]);
                    restoreColors(s, kept, Swatch.WHITE, Swatch.PUPIL, Swatch.LID);
                }
            }
        }
        setStatus(I18n.format("salamander.expression.fixed"), true);
        this.note = null;
    }

    // Colors

    private void openPopup(Swatch swatch) {
        if (this.popup == swatch) {
            closePopup();
            return;
        }
        this.popup = swatch;
        this.popupBrush = swatch == Swatch.LID && this.paintLids || swatch == Swatch.BROW && this.paintBrows;
        if (!this.popupBrush) remember();
        int[] colors = ExpressionDraft.colors(this.data, swatch, this.side);
        setPickerColor(
            this.popupBrush ? swatch == Swatch.LID ? this.lidBrush : this.browBrush
                : colors.length > 0 ? colors[0] : 0xffffffff);
    }

    private void closePopup() {
        this.popup = null;
        this.pickerDrag = 0;
    }

    /** Gives the open swatch this color: the brush's, or every pixel of the feature on the sides edited. */
    private void applyColor(int color) {
        if (this.popup == null) return;
        color |= 0xff000000;
        if (this.popupBrush) {
            if (this.popup == Swatch.LID) this.lidBrush = color;
            else this.browBrush = color;
        } else {
            for (int s : sides(this.side)) {
                ExpressionDraft.paint(this.data, this.popup, s, color);
                this.edited[this.popup.ordinal()][s] = true;
            }
        }
        refresh();
    }

    private void setPickerColor(int color) {
        float[] hsb = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
        if (hsb[1] > 0.0f) this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        if (this.hexField != null) this.hexField.setText(String.format("#%06X", color & 0xffffff));
    }

    private int pickerColor() {
        return 0xff000000 | Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 0xffffff;
    }

    private int popupX() {
        return this.controlsX;
    }

    private int popupY() {
        return this.controlsY;
    }

    // Input

    @Override
    protected void actionPerformed(GuiButton button) {
        int id = button.id;
        if (id >= FIX) {
            int index = id - FIX;
            if (index < this.problems.size()) applyFix(this.problems.get(index));
        } else if (id >= TAB && id < TAB + 3) {
            this.tab = Tab.values()[id - TAB];
            this.note = null;
            closePopup();
        } else if (id >= SHAPE && id < SHAPE + ExpressionDraft.SHAPES.length) {
            setShape(ExpressionDraft.SHAPES[id - SHAPE]);
        } else if (id >= BROW_SHAPE && id < BROW_SHAPE + 3) {
            setBrows(Brows.values()[id - BROW_SHAPE], browRowOrDefault());
        } else if (swatchOf(id) != null) {
            openPopup(swatchOf(id));
        } else {
            switch (id) {
                case BACK:
                    this.back.run();
                    return;
                case SAVE:
                    exportSkin();
                    break;
                case UPLOAD:
                    confirmWawelAuthUpload();
                    return;
                case EYE_UP:
                case EYE_DOWN:
                    moveEyes(id == EYE_UP ? -1 : 1);
                    break;
                case MIRROR:
                case BROW_MIRROR:
                    this.mirror = !this.mirror;
                    break;
                case PICK:
                    this.pickMode = !this.pickMode;
                    this.paintLids &= !this.pickMode;
                    this.note = null;
                    break;
                case DETECT:
                    remember();
                    this.note = null;
                    detect();
                    break;
                case PAINT_LIDS:
                    this.paintLids = !this.paintLids;
                    this.pickMode &= !this.paintLids;
                    if (this.paintLids) {
                        int[] colors = ExpressionDraft.colors(this.data, Swatch.LID, this.side);
                        if (colors.length > 0) this.lidBrush = colors[0];
                    }
                    closePopup();
                    break;
                case PAINT_BROWS:
                    this.paintBrows = !this.paintBrows;
                    if (this.paintBrows) {
                        int[] colors = ExpressionDraft.colors(this.data, Swatch.BROW, this.side);
                        if (colors.length > 0) this.browBrush = colors[0];
                    }
                    closePopup();
                    break;
                case MOVING_BROWS:
                    remember();
                    if (ExpressionDraft.brows(this.data) != null) {
                        ExpressionDraft.removeBrows(this.data);
                        this.paintBrows = false;
                    } else {
                        ExpressionDraft.placeBrows(this.data, this.skin, Brows.PAIRED, browRowOrDefault(), -1);
                    }
                    break;
                case BROW_UP:
                case BROW_DOWN:
                    setBrows(Brows.PAIRED, browRowOrDefault() + (id == BROW_UP ? -1 : 1));
                    break;
                case OUTER: {
                    boolean on = ExpressionDraft.hasOuterBrows(this.data, this.side);
                    setOuterBrows(!on, browRowOrDefault());
                    break;
                }
                case OUTER_UP:
                case OUTER_DOWN:
                    setOuterBrows(true, ExpressionDraft.outerRow(this.data, this.side) + (id == OUTER_UP ? -1 : 1));
                    break;
                case LASHES: {
                    remember();
                    boolean on = ExpressionDraft.hasLashes(this.data, this.side);
                    for (int s : sides(this.side)) ExpressionDraft.setLashes(this.data, s, !on);
                    break;
                }
                case BLINK:
                    this.blinkAt = Minecraft.getSystemTime();
                    break;
                case LOOK:
                    this.lookAt = Minecraft.getSystemTime();
                    break;
                case SURPRISED:
                    this.surprisedAt = Minecraft.getSystemTime();
                    break;
                case POPUP_DONE:
                    closePopup();
                    break;
                default:
                    break;
            }
        }
        refresh();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        boolean overButton = false;
        for (Object object : this.buttonList) {
            GuiButton b = (GuiButton) object;
            overButton |= b.visible && b.enabled
                && mouseX >= b.xPosition
                && mouseY >= b.yPosition
                && mouseX < b.xPosition + b.width
                && mouseY < b.yPosition + b.height;
        }
        super.mouseClicked(mouseX, mouseY, button);
        if (overButton || !ExpressionDraft.usable(this.skin)) return;
        if (this.popup != null) {
            this.hexField.mouseClicked(mouseX, mouseY, button);
            int x = popupX() + 6, y = popupY() + 26;
            if (inside(mouseX, mouseY, x, y, PICKER_WIDTH, PICKER_HEIGHT)) {
                this.pickerDrag = 1;
                pickSaturation(mouseX, mouseY);
                return;
            }
            if (inside(mouseX, mouseY, x + PICKER_WIDTH + 4, y, HUE_WIDTH, PICKER_HEIGHT)) {
                this.pickerDrag = 2;
                pickHue(mouseY);
                return;
            }
            int fx = faceX(mouseX), fy = faceY(mouseY);
            if (fx >= 0 && fy >= 0) {
                int[][] face = ExpressionFace.colors(this.skin, this.data, 0, 0, 0, 0, true);
                setPickerColor(face[fx][fy]);
                applyColor(face[fx][fy]);
                return;
            }
            if (!inside(mouseX, mouseY, popupX(), popupY(), 182, 120)) {
                closePopup();
                refresh();
            }
            return;
        }
        if (inside(mouseX, mouseY, this.headX, this.headY, this.headSize, this.headSize)) {
            this.turning = true;
            this.pressX = mouseX;
            this.pressY = mouseY;
            this.pressYaw = this.yaw;
            this.pressPitch = this.pitch;
            return;
        }
        int fx = faceX(mouseX), fy = faceY(mouseY);
        if (fx < 0 || fy < 0) return;
        int clickedSide = fx < 4 ? 0 : 1;
        if (!this.mirror) this.side = clickedSide;
        if (this.tab == Tab.EYES) {
            if (this.pickMode) {
                pickPupil(fx, fy);
            } else if (this.paintLids) {
                remember();
                this.painting = true;
                this.paintValue = button != 1;
                paint(fx, fy);
            } else {
                this.dragX = fx;
                this.dragY = fy;
                this.dragToX = fx;
                this.dragToY = fy;
            }
        } else if (this.tab == Tab.BROWS) {
            if (this.paintBrows) {
                remember();
                this.painting = true;
                this.paintValue = button != 1;
                paint(fx, fy);
            } else {
                Brows brows = ExpressionDraft.brows(this.data);
                setBrows(brows == null ? Brows.PAIRED : brows, fy);
                if (brows != null && brows != Brows.PAIRED && fy != 3)
                    this.note = I18n.format("salamander.expression.browRow3");
            }
        } else {
            if (ExpressionDraft.hasOuterBrows(this.data, this.side) || this.mirror) setOuterBrows(true, fy);
        }
        refresh();
    }

    private void paint(int x, int y) {
        if (this.tab == Tab.EYES) {
            if (this.data.eyes[x][y] == 0 || !(this.mirror || (x < 4 ? 0 : 1) == this.side)) return;
            int color = this.paintValue ? this.lidBrush : ExpressionDraft.skinColor(this.skin);
            ExpressionDraft.paintPixel(this.data, Swatch.LID, x, y, color);
            if (this.mirror) ExpressionDraft.paintPixel(this.data, Swatch.LID, 7 - x, y, color);
        } else {
            if (this.data.brows[x][y] == 0) return;
            int color = this.paintValue ? this.browBrush : ExpressionSkinExporter.base(this.skin, x, y);
            ExpressionDraft.paintPixel(this.data, Swatch.BROW, x, y, color);
            if (this.mirror && this.data.brows[7 - x][y] != 0) ExpressionDraft.paintPixel(
                this.data,
                Swatch.BROW,
                7 - x,
                y,
                this.paintValue ? color : ExpressionSkinExporter.base(this.skin, 7 - x, y));
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long elapsed) {
        if (this.pickerDrag == 1) {
            pickSaturation(mouseX, mouseY);
            return;
        }
        if (this.pickerDrag == 2) {
            pickHue(mouseY);
            return;
        }
        // Turning follows the pointer every frame in drawScreen; input events set the same target between frames.
        if (this.turning) {
            turnTo(mouseX, mouseY);
            return;
        }
        int fx = faceX(mouseX), fy = faceY(mouseY);
        if (this.painting) {
            if (fx >= 0 && fy >= 0) {
                paint(fx, fy);
                refresh();
            }
            return;
        }
        if (this.dragX >= 0) {
            this.dragToX = Math.max(0, Math.min(7, (mouseX - this.canvasX) / this.px));
            this.dragToY = Math.max(0, Math.min(7, (mouseY - this.canvasY) / this.px));
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        super.mouseMovedOrUp(mouseX, mouseY, button);
        if (button < 0) return;
        this.pickerDrag = 0;
        this.turning = false;
        if (this.painting) {
            this.painting = false;
            refresh();
        }
        if (this.dragX < 0) return;
        int x0 = this.dragX, y0 = this.dragY, x1 = this.dragToX, y1 = this.dragToY;
        this.dragX = -1;
        this.note = null;
        if (x0 == x1 && y0 == y1) clickEyes(x0, y0);
        else dropEyes(x0, y0, x1, y1);
        refresh();
    }

    /** A click on the face: on an eye it picks that side, elsewhere it moves the eyes to that row. */
    private void clickEyes(int x, int y) {
        if (this.data.eyes[x][y] != 0) return;
        EyeType shape = ExpressionDraft.shape(this.data, this.side);
        if (shape == null) shape = EyeType.NORMAL_2X1;
        remember();
        placeEyes(shape, Math.max(shape.minRow, Math.min(shape.maxRow, y)), this.side);
        if (y < shape.minRow || y > shape.maxRow)
            this.note = I18n.format("salamander.expression.note.rows", shapeName(shape), shape.minRow, shape.maxRow);
    }

    /** A drag over the face: the eyes take the supported shape and row nearest to the dragged box. */
    private void dropEyes(int x0, int y0, int x1, int y1) {
        int[] snapped = snap(x0, y0, x1, y1);
        if (snapped == null) {
            this.note = I18n.format("salamander.expression.note.snap");
            return;
        }
        remember();
        placeEyes(ExpressionDraft.SHAPES[snapped[0]], snapped[1], x0 < 4 ? 0 : 1);
    }

    /** The shape (index into SHAPES) and row nearest a dragged box, or null if none is near. */
    private static int[] snap(int x0, int y0, int x1, int y1) {
        int side = x0 < 4 ? 0 : 1;
        int left = Math.min(x0, x1), right = Math.max(x0, x1), top = Math.min(y0, y1), bottom = Math.max(y0, y1);
        if (side == 1) {
            int l = 7 - right;
            right = 7 - left;
            left = l;
        }
        right = Math.min(right, 3);
        double best = 0.3;
        int[] result = null;
        for (int i = 0; i < ExpressionDraft.SHAPES.length; i++) {
            EyeType type = ExpressionDraft.SHAPES[i];
            for (int row = type.minRow; row <= type.maxRow; row++) {
                int ix = Math.min(right, type.x + type.width - 1) - Math.max(left, type.x) + 1;
                int iy = Math.min(bottom, row + type.height - 1) - Math.max(top, row) + 1;
                if (ix <= 0 || iy <= 0) continue;
                int intersection = ix * iy;
                int union = (right - left + 1) * (bottom - top + 1) + type.width * type.height - intersection;
                double overlap = (double) intersection / union;
                if (overlap > best) {
                    best = overlap;
                    result = new int[] { i, row };
                }
            }
        }
        return result;
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (isCtrlKeyDown() && !this.hexField.isFocused() && (key == Keyboard.KEY_Z || key == Keyboard.KEY_Y)) {
            undo(key == Keyboard.KEY_Y || isShiftKeyDown());
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) {
            if (this.popup != null) {
                closePopup();
                refresh();
            } else {
                this.back.run();
            }
            return;
        }
        if (this.hexField.isFocused()) {
            this.hexField.textboxKeyTyped(character, key);
            String value = this.hexField.getText()
                .trim();
            if (value.matches("#[0-9a-fA-F]{6}")) {
                int color = 0xff000000 | Integer.parseInt(value.substring(1), 16);
                float[] hsb = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
                if (hsb[1] > 0.0f) this.hue = hsb[0];
                this.saturation = hsb[1];
                this.brightness = hsb[2];
                applyColor(color);
            }
        }
    }

    private void pickSaturation(int mouseX, int mouseY) {
        this.saturation = clamp((float) (mouseX - popupX() - 6) / (PICKER_WIDTH - 1));
        this.brightness = 1.0f - clamp((float) (mouseY - popupY() - 26) / (PICKER_HEIGHT - 1));
        this.hexField.setText(String.format("#%06X", pickerColor() & 0xffffff));
        applyColor(pickerColor());
    }

    private void pickHue(int mouseY) {
        this.hue = clamp((float) (mouseY - popupY() - 26) / (PICKER_HEIGHT - 1));
        this.hexField.setText(String.format("#%06X", pickerColor() & 0xffffff));
        applyColor(pickerColor());
    }

    @Override
    public void updateScreen() {
        this.hexField.updateCursorCounter();
        // Wawel Auth may hear from the provider whether it takes skins only after the editor opened.
        for (Object object : this.buttonList) {
            GuiButton button = (GuiButton) object;
            if (button.id == UPLOAD)
                button.visible = uploadTarget() != null && WawelAuthUpload.skinUploadEnabled(uploadTarget());
        }
    }

    // Drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.drawDefaultBackground();
        String title = this.name == null ? I18n.format("salamander.expression.title")
            : I18n.format("salamander.expression.titleFor", this.name);
        this.drawCenteredString(this.fontRendererObj, title, this.width / 2, this.canvasY - 13, 0xffffff);
        long now = Minecraft.getSystemTime();
        moveHead(mouseX, mouseY);
        int[] look = look(now, mouseX, mouseY);
        float blink = blink(now);
        int lift = lift(now, mouseX, mouseY);
        if (ExpressionDraft.usable(this.skin)) {
            drawHead(mouseX, mouseY, look, blink, lift);
            drawCanvas(mouseX, mouseY, look, blink, lift);
            drawControlsText();
        }
        for (String[] line : this.infoLines) this.fontRendererObj
            .drawString(line[0], this.infoX, Integer.parseInt(line[2]), Integer.parseInt(line[1], 16));
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.popup != null) drawPopup();
        drawTooltips(mouseX, mouseY);
    }

    /**
     * Moves the head for this frame. A drag turns it by how far the pointer is from where the drag started, read each
     * frame rather than with the game's slower input ticks; the pointer's place anywhere on the screen turns it a
     * little toward it, saturating smoothly. Both ease toward their targets by real time, whatever the frame rate.
     */
    private void moveHead(int mouseX, int mouseY) {
        long now = System.nanoTime();
        float seconds = this.lastFrameNanos == 0 ? 0 : Math.min(0.1f, (now - this.lastFrameNanos) / 1.0e9f);
        this.lastFrameNanos = now;
        if (this.turning && Mouse.isButtonDown(0)) turnTo(mouseX, mouseY);
        float cx = this.headX + this.headSize / 2.0f, cy = this.headY + this.headSize / 2.0f;
        float reach = Math.max(this.width, this.height) / 3.0f;
        float lookYawTarget = LOOK_YAW * (float) Math.tanh((mouseX - cx) / reach);
        float lookPitchTarget = LOOK_PITCH * (float) Math.tanh((mouseY - cy) / reach);
        float turnEase = 1.0f - (float) Math.exp(-seconds / TURN_EASE_SECONDS);
        float lookEase = 1.0f - (float) Math.exp(-seconds / LOOK_EASE_SECONDS);
        this.shownYaw += (this.yaw - this.shownYaw) * turnEase;
        this.shownPitch += (this.pitch - this.shownPitch) * turnEase;
        this.lookYaw += (lookYawTarget - this.lookYaw) * lookEase;
        this.lookPitch += (lookPitchTarget - this.lookPitch) * lookEase;
    }

    /** The turn for the pointer at x, y during a drag: by how far it is from where the drag started. */
    private void turnTo(int x, int y) {
        this.yaw = this.pressYaw + (x - this.pressX) * TURN_PER_PIXEL;
        this.pitch = Math.max(-60.0f, Math.min(60.0f, this.pressPitch + (y - this.pressY) * TURN_PER_PIXEL));
    }

    /** Where the pupils look: the moves' sequences, or toward the mouse from the head. */
    private int[] look(long now, int mouseX, int mouseY) {
        if (within(now - this.surprisedAt, SURPRISE_MS)) return new int[] { 0, 0 };
        long looking = now - this.lookAt;
        if (within(looking, LOOK_STEP_MS * 4)) {
            int[][] steps = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
            return steps[(int) (looking / LOOK_STEP_MS)];
        }
        int cx = this.headX + this.headSize / 2, cy = this.headY + this.headSize / 2;
        float reach = this.headSize * 0.4f;
        int x = mouseX - cx > reach ? 1 : mouseX - cx < -reach ? -1 : 0;
        int y = mouseY - cy > reach ? 1 : mouseY - cy < -reach ? -1 : 0;
        return new int[] { x, y };
    }

    private float blink(long now) {
        if (within(now - this.surprisedAt, SURPRISE_MS)) return 0;
        long since = now - this.blinkAt;
        if (within(since, BLINK_MS)) return triangle(since);
        long auto = now % AUTO_BLINK_MS;
        return auto < BLINK_MS ? triangle(auto) : 0;
    }

    /** Whether a move started since ago still runs; a move never started is long gone. */
    private static boolean within(long since, long duration) {
        return since >= 0 && since < duration;
    }

    private static float triangle(long elapsed) {
        float phase = (float) elapsed / BLINK_MS;
        return phase < 0.5f ? phase * 2 : (1 - phase) * 2;
    }

    private int lift(long now, int mouseX, int mouseY) {
        return within(now - this.surprisedAt, SURPRISE_MS)
            || inside(mouseX, mouseY, this.headX, this.headY, this.headSize, this.headSize) ? 1 : 0;
    }

    /** Whether the face shows the draft at rest with outlines, for editing, rather than moving. */
    private boolean editing(int mouseX, int mouseY) {
        return this.popup != null || this.pickMode
            || this.paintLids && this.tab == Tab.EYES
            || this.paintBrows && this.tab == Tab.BROWS
            || this.dragX >= 0
            || inside(mouseX, mouseY, this.canvasX, this.canvasY, this.px * 8, this.px * 8);
    }

    private void drawCanvas(int mouseX, int mouseY, int[] look, float blink, int lift) {
        boolean editing = editing(mouseX, mouseY);
        boolean lids = this.paintLids && this.tab == Tab.EYES;
        // With the pack drawing the preview, the face stays still: the pack shows how it moves.
        int[][] face = editing || this.packPreview
            ? ExpressionFace.colors(this.skin, this.data, 0, 0, lids ? 1 : 0, 0, true)
            : ExpressionFace.colors(this.skin, this.data, look[0], look[1], blink, lift, true);
        int size = this.px * 8;
        Gui.drawRect(this.canvasX - 1, this.canvasY - 1, this.canvasX + size + 1, this.canvasY + size + 1, 0xff000000);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) {
            int left = this.canvasX + x * this.px, top = this.canvasY + y * this.px;
            Gui.drawRect(left, top, left + this.px, top + this.px, 0xff000000 | face[x][y]);
        }
        if (editing) for (int i = 0; i <= 8; i++) {
            Gui.drawRect(
                this.canvasX + i * this.px,
                this.canvasY,
                this.canvasX + i * this.px + 1,
                this.canvasY + size,
                0x33000000);
            Gui.drawRect(
                this.canvasX,
                this.canvasY + i * this.px,
                this.canvasX + size,
                this.canvasY + i * this.px + 1,
                0x33000000);
        }
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) {
            int s = x < 4 ? 0 : 1;
            boolean active = this.mirror || s == this.side;
            int outline = 0;
            if (this.tab == Tab.EYES) {
                if (this.pickMode && this.data.pupils[x][y]) outline = 0xff55aaff;
                else if (this.data.eyes[x][y] != 0) outline = active ? 0xff55ff55 : 0xff2f7f2f;
            } else if (this.tab == Tab.BROWS) {
                if (this.data.brows[x][y] != 0) outline = active ? 0xffffaa33 : 0xff7f5519;
            } else {
                if (this.data.overlayBrows[x][y] != 0) outline = active ? 0xff55ddff : 0xff2a6e7f;
                else if ((this.data.lashes[x][y] >>> 24) != 0) outline = active ? 0xffff66cc : 0xff7f3366;
            }
            if (outline != 0) outline(x, y, x, y, outline);
        }
        if (this.dragX >= 0 && (this.dragToX != this.dragX || this.dragToY != this.dragY)) {
            int[] snapped = snap(this.dragX, this.dragY, this.dragToX, this.dragToY);
            if (snapped == null) {
                outline(
                    Math.min(this.dragX, this.dragToX),
                    Math.min(this.dragY, this.dragToY),
                    Math.max(this.dragX, this.dragToX),
                    Math.max(this.dragY, this.dragToY),
                    0xffff4444);
            } else {
                EyeType type = ExpressionDraft.SHAPES[snapped[0]];
                for (int s : sides(this.dragX < 4 ? 0 : 1)) {
                    int first = s == 0 ? type.x : 8 - type.x - type.width;
                    outline(first, snapped[1], first + type.width - 1, snapped[1] + type.height - 1, 0xffffff55);
                }
            }
        }
    }

    private void outline(int x0, int y0, int x1, int y1, int color) {
        int left = this.canvasX + x0 * this.px, top = this.canvasY + y0 * this.px;
        int right = this.canvasX + (x1 + 1) * this.px, bottom = this.canvasY + (y1 + 1) * this.px;
        Gui.drawRect(left, top, right, top + 1, color);
        Gui.drawRect(left, bottom - 1, right, bottom, color);
        Gui.drawRect(left, top, left + 1, bottom, color);
        Gui.drawRect(right - 1, top, right, bottom, color);
    }

    private void drawControlsText() {
        if (this.popup != null) return;
        int x = this.controlsX, y = this.controlsY;
        if (this.tab == Tab.EYES) {
            this.fontRendererObj.drawString(I18n.format("salamander.expression.shape"), x, y + 18, 0xa0a0a0);
            int row = ExpressionDraft.eyeRow(this.data, this.side);
            this.fontRendererObj
                .drawString(row < 0 ? "-" : I18n.format("salamander.expression.row", row), x, y + 53, 0xe0e0e0);
        } else if (this.tab == Tab.BROWS) {
            this.fontRendererObj.drawString(I18n.format("salamander.expression.shape"), x, y + 41, 0xa0a0a0);
            int row = ExpressionDraft.browRow(this.data, this.side);
            this.fontRendererObj
                .drawString(row < 0 ? "-" : I18n.format("salamander.expression.row", row), x, y + 61, 0xe0e0e0);
        } else {
            if (!ExpressionDraft.lashesFit(this.data, this.side)) {
                int line = y + 94;
                for (Object text : this.fontRendererObj
                    .listFormattedStringToWidth(I18n.format("salamander.expression.lashes.unavailable"), 182)) {
                    this.fontRendererObj.drawString((String) text, x, line, 0xa0a0a0);
                    line += 10;
                }
            }
        }
    }

    /**
     * The preview: the pack's own model wearing the draft when a pack with eyes draws players and there is a world to
     * draw it in, else the editor's head moving as those packs do.
     */
    private void drawHead(int mouseX, int mouseY, int[] look, float blink, int lift) {
        boolean hovered = inside(mouseX, mouseY, this.headX, this.headY, this.headSize, this.headSize);
        Gui.drawRect(
            this.headX,
            this.headY,
            this.headX + this.headSize,
            this.headY + this.headSize,
            hovered ? 0xff5a5a5a : 0xff404040);
        Gui.drawRect(
            this.headX + 1,
            this.headY + 1,
            this.headX + this.headSize - 1,
            this.headY + this.headSize - 1,
            0xff101418);
        int scale = new net.minecraft.client.gui.ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight)
            .getScaleFactor();
        boolean pack = this.preview
            .prepare(ExpressionPreview.world(this.previewPlayer), this.previewSkin, this.slim, this.name)
            && this.preview.packShown();
        if (pack != this.packPreview) {
            this.packPreview = pack;
            refresh();
        }
        if (pack) {
            this.preview.draw(
                this.headX + 1,
                this.headY + 1,
                this.headSize - 2,
                this.shownYaw,
                this.shownPitch,
                this.lookYaw,
                this.lookPitch,
                scale);
            return;
        }
        int[][] face = ExpressionFace.colors(this.skin, this.data, look[0], look[1], blink, lift, false);
        this.head.draw(
            this.headX + this.headSize / 2,
            this.headY + this.headSize / 2,
            this.headSize,
            this.shownYaw + this.lookYaw,
            this.shownPitch + this.lookPitch,
            this.skin,
            face,
            scale);
    }

    private void drawPopup() {
        int x = popupX(), y = popupY(), width = 182, height = 120;
        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, 0xffa0a0a0);
        Gui.drawRect(x, y, x + width, y + height, 0xff181818);
        String title = I18n.format(
            "salamander.expression.swatch." + this.popup.name()
                .toLowerCase(java.util.Locale.ROOT));
        if (this.popupBrush) title = I18n.format("salamander.expression.brush", title);
        this.fontRendererObj.drawString(title, x + 6, y + 6, 0xffffff);
        int px0 = x + 6, py0 = y + 26;
        for (int dx = 0; dx < PICKER_WIDTH; dx += 2) {
            float sat = (float) dx / (PICKER_WIDTH - 1);
            for (int dy = 0; dy < PICKER_HEIGHT; dy += 2) {
                float value = 1.0f - (float) dy / (PICKER_HEIGHT - 1);
                Gui.drawRect(
                    px0 + dx,
                    py0 + dy,
                    px0 + Math.min(dx + 2, PICKER_WIDTH),
                    py0 + Math.min(dy + 2, PICKER_HEIGHT),
                    0xff000000 | Color.HSBtoRGB(this.hue, sat, value) & 0xffffff);
            }
        }
        int hueX = px0 + PICKER_WIDTH + 4;
        for (int dy = 0; dy < PICKER_HEIGHT; dy++) Gui.drawRect(
            hueX,
            py0 + dy,
            hueX + HUE_WIDTH,
            py0 + dy + 1,
            0xff000000 | Color.HSBtoRGB((float) dy / (PICKER_HEIGHT - 1), 1.0f, 1.0f) & 0xffffff);
        int markerX = px0 + Math.round(this.saturation * (PICKER_WIDTH - 1));
        int markerY = py0 + Math.round((1.0f - this.brightness) * (PICKER_HEIGHT - 1));
        Gui.drawRect(markerX - 2, markerY - 2, markerX + 3, markerY + 3, 0xffffffff);
        Gui.drawRect(markerX - 1, markerY - 1, markerX + 2, markerY + 2, pickerColor());
        int hueY = py0 + Math.round(this.hue * (PICKER_HEIGHT - 1));
        Gui.drawRect(hueX - 1, hueY - 1, hueX + HUE_WIDTH + 1, hueY + 2, 0xffffffff);
        Gui.drawRect(hueX + HUE_WIDTH + 6, py0, hueX + HUE_WIDTH + 40, py0 + PICKER_HEIGHT, 0xffffffff);
        Gui.drawRect(hueX + HUE_WIDTH + 7, py0 + 1, hueX + HUE_WIDTH + 39, py0 + PICKER_HEIGHT - 1, pickerColor());
        this.fontRendererObj.drawString(I18n.format("salamander.expression.hex"), x + 6, y + 79, 0xa0a0a0);
        this.hexField.drawTextBox();
        int line = y + 94;
        for (Object text : this.fontRendererObj
            .listFormattedStringToWidth(I18n.format("salamander.expression.hint.popup"), 118)) {
            this.fontRendererObj.drawString((String) text, x + 6, line, 0xa0a0a0);
            line += 9;
        }
        // The Done button is drawn with the others, under the popup's box; draw it again on top.
        for (Object object : this.buttonList) {
            GuiButton button = (GuiButton) object;
            if (button.id == POPUP_DONE) button.drawButton(this.mc, this.lastMouseX, this.lastMouseY);
        }
    }

    private void drawTooltips(int mouseX, int mouseY) {
        for (Object object : this.buttonList) {
            GuiButton button = (GuiButton) object;
            if (!button.visible || mouseX < button.xPosition
                || mouseY < button.yPosition
                || mouseX >= button.xPosition + button.width
                || mouseY >= button.yPosition + button.height) continue;
            String tooltip = button instanceof FlatButton ? ((FlatButton) button).tooltip : null;
            if (button.id == UPLOAD) {
                Account target = uploadTarget();
                tooltip = target == null ? I18n.format("salamander.upload.noAccount")
                    : target.offline ? I18n.format("salamander.expression.upload.offline")
                        : I18n.format("salamander.expression.upload.tooltip", target.provider);
            }
            if (button.id == SAVE || button.id == UPLOAD) {
                if (!this.problems.isEmpty()) tooltip = I18n.format("salamander.expression.fixFirst");
            }
            if (tooltip != null)
                this.func_146283_a(this.fontRendererObj.listFormattedStringToWidth(tooltip, 220), mouseX, mouseY);
            return;
        }
        boolean hintShown = !this.infoLines.isEmpty() && this.infoLines.get(0)[1].equals("bfbfbf");
        if (this.dragX < 0 && !hintShown
            && inside(mouseX, mouseY, this.canvasX, this.canvasY, this.px * 8, this.px * 8)) {
            this.func_146283_a(this.fontRendererObj.listFormattedStringToWidth(hint(), 160), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, this.headX, this.headY, this.headSize, this.headSize)) this.func_146283_a(
            this.fontRendererObj.listFormattedStringToWidth(I18n.format("salamander.expression.head.tooltip"), 200),
            mouseX,
            mouseY);
    }

    // Saving

    private void exportSkin() {
        BufferedImage output = generatedSkin();
        if (output == null) return;
        FileDialog dialog = new FileDialog(
            (Frame) null,
            I18n.format("salamander.expression.saveDialog"),
            FileDialog.SAVE);
        dialog.setFile(safeName(this.name) + "_expressions.png");
        dialog.setVisible(true);
        String filename = dialog.getFile();
        String directory = dialog.getDirectory();
        dialog.dispose();
        if (filename == null) {
            setStatus(I18n.format("salamander.expression.cancelled"), false);
            return;
        }
        File file = new File(directory, filename);
        if (!file.getName()
            .toLowerCase()
            .endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        try {
            ImageIO.write(output, "png", file);
            setStatus(I18n.format("salamander.expression.saved", file.getName()), true);
        } catch (IOException exception) {
            setStatus(I18n.format("salamander.expression.saveFailed", exception.getMessage()), false);
        }
    }

    private BufferedImage generatedSkin() {
        try {
            return ExpressionSkinExporter.export(this.skin, this.data);
        } catch (IllegalArgumentException exception) {
            setStatus(exception.getMessage(), false);
            return null;
        }
    }

    private void confirmWawelAuthUpload() {
        if (generatedSkin() == null) return;
        Account target = uploadTarget();
        if (target == null) {
            setStatus(I18n.format("salamander.upload.noAccount"), false);
            refresh();
            return;
        }
        this.mc.displayGuiScreen(new UploadConfirmScreen(target, confirmed -> {
            this.mc.displayGuiScreen(this);
            if (!confirmed) {
                setStatus(I18n.format("salamander.upload.cancelled"), false);
                refresh();
                return;
            }
            startWawelAuthUpload(target);
        }));
    }

    private void startWawelAuthUpload(Account target) {
        BufferedImage output = generatedSkin();
        if (output == null) return;
        this.uploading = true;
        setStatus(I18n.format("salamander.upload.progress", target.name, target.provider), false);
        refresh();
        WawelAuthUpload.upload(target, output, this.slim, (result, error) -> {
            this.uploading = false;
            if (error != null) setStatus(I18n.format("salamander.upload.failed", error), false);
            else setStatus(result == null ? I18n.format("salamander.upload.success") : result, true);
            refresh();
        });
    }

    // Geometry

    private int faceX(int mouseX) {
        int relative = mouseX - this.canvasX;
        return relative >= 0 && relative < this.px * 8 ? relative / this.px : -1;
    }

    private int faceY(int mouseY) {
        int relative = mouseY - this.canvasY;
        return relative >= 0 && relative < this.px * 8 ? relative / this.px : -1;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static String safeName(String name) {
        return name == null ? "player" : name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        this.head.release();
        this.preview.release();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
