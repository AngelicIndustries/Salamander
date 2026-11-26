package industries.angelic.salamander.cem.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.cem.model.CemModel;
import industries.angelic.salamander.config.CemConfig;
import industries.angelic.salamander.mixins.early.minecraft.client.AccessorMinecraft;

/**
 * The face of a player's pack model, for a mod that draws the player's body itself, as Even Mo' Bends does. A pack
 * such as Just Expressions keeps the vanilla body and only adds to the head, and animates only what it adds: that
 * face draws on the host model's head with the pack's own animations, and the host keeps the body. A pack that changes
 * or animates anything else, as Fresh Moves does, covers the body: it draws the whole player as before.
 */
public final class CemFaces {

    public enum Coverage {
        NONE,
        FACE,
        BODY
    }

    /** What a pack player model covers, worked out once per loaded model (a reload loads new ones). */
    static final class Face {

        final Coverage coverage;
        /** The nodes drawn on the host's head, and those of them whose own boxes are the vanilla head's or hat's. */
        final List<CemModel.Node> roots;
        final Set<CemModel.Node> skipped;
        /** Why the model covers the body; null for a face. */
        final String reason;

        Face(Coverage coverage, List<CemModel.Node> roots, Set<CemModel.Node> skipped, String reason) {
            this.coverage = coverage;
            this.roots = roots;
            this.skipped = skipped;
            this.reason = reason;
        }
    }

    private static final float EPSILON = 0.001f;
    /**
     * The modern vanilla player, part by part in the part's own frame, as OptiFine's template player model has it:
     * x, y, z, width, height, depth, u, v, inflation, and for an arm or sleeve the slim box's x (its width one less).
     */
    private static final Map<String, float[]> VANILLA = new HashMap<>();
    private static final float WIDE = Float.NaN;

    static {
        VANILLA.put("head", new float[] { -4, -8, -4, 8, 8, 8, 0, 0, 0, WIDE });
        VANILLA.put("headwear", new float[] { -4, -8, -4, 8, 8, 8, 32, 0, 0.5f, WIDE });
        VANILLA.put("body", new float[] { -4, 0, -2, 8, 12, 4, 16, 16, 0, WIDE });
        VANILLA.put("jacket", new float[] { -4, 0, -2, 8, 12, 4, 16, 32, 0.25f, WIDE });
        VANILLA.put("right_arm", new float[] { -3, -2, -2, 4, 12, 4, 40, 16, 0, -2 });
        VANILLA.put("right_sleeve", new float[] { -3, -2, -2, 4, 12, 4, 40, 32, 0.25f, -2 });
        VANILLA.put("left_arm", new float[] { -1, -2, -2, 4, 12, 4, 32, 48, 0, -1 });
        VANILLA.put("left_sleeve", new float[] { -1, -2, -2, 4, 12, 4, 48, 48, 0.25f, -1 });
        VANILLA.put("right_leg", new float[] { -2, 0, -2, 4, 12, 4, 0, 16, 0, WIDE });
        VANILLA.put("right_pants", new float[] { -2, 0, -2, 4, 12, 4, 0, 32, 0.25f, WIDE });
        VANILLA.put("left_leg", new float[] { -2, 0, -2, 4, 12, 4, 16, 48, 0, WIDE });
        VANILLA.put("left_pants", new float[] { -2, 0, -2, 4, 12, 4, 0, 48, 0.25f, WIDE });
        VANILLA.put("cloak", new float[] { -5, 0, -1, 10, 16, 1, 0, 0, 0, WIDE });
    }

    /** The parts whose additions belong to the head: what a pack adds there is its face. */
    private static final List<String> HEAD = java.util.Arrays.asList("head", "headwear", "left_ear", "right_ear");

    private static final Map<CemModel, Face> FACES = Collections.synchronizedMap(new WeakHashMap<>());
    /** A vanilla player renderer: pack player models bind to its model here, as they do to the game's. */
    private static RenderPlayer renderer;

    private CemFaces() {}

    /** What the pack model the player draws with covers, NONE for no pack model or another entity. */
    public static Coverage coverage(Entity entity) {
        CemResources.Selection selection = select(entity);
        return selection == null ? Coverage.NONE : face(selection.entry.model).coverage;
    }

    /** The resource pack the player's pack model comes from, or null for none. */
    public static String packName(Entity entity) {
        CemResources.Selection selection = select(entity);
        return selection == null ? null : CemResources.packName(selection.entry.model.source);
    }

    /**
     * Draws the face of the player's pack model at the current GL matrix, which is the host model's head bone, posed
     * by the pack's animations for this frame. Whether there was a face to draw: false unless the pack covers only it.
     */
    public static boolean draw(Entity entity, float limbSwing, float limbSpeed, float age, float headYaw,
        float headPitch, float scale) {
        if (!(entity instanceof AbstractClientPlayer)) return false;
        AbstractClientPlayer player = (AbstractClientPlayer) entity;
        ModelBiped model = model();
        CemBinding binding = CemRuntime.bindingFor(model, player);
        ResourceLocation skin = player.getLocationSkin();
        CemResources.Selection selection = select(player, model, binding, skin);
        if (selection == null) return false;
        Face face = face(selection.entry.model);
        if (face.coverage != Coverage.FACE || face.roots.isEmpty()) return false;
        // The native pose a pack reads, as vanilla's model has it this frame.
        model.setRotationAngles(limbSwing, limbSpeed, age, headYaw, headPitch, scale, player);
        float partial = ((AccessorMinecraft) Minecraft.getMinecraft()).salamander$getTimer().renderPartialTicks;
        CemRuntime.Draw draw = new CemRuntime.Draw(
            binding,
            selection.entry,
            player,
            partial,
            limbSwing,
            limbSpeed,
            age,
            headYaw,
            headPitch,
            skin);
        draw.inputs.put("rule_index", (double) selection.rule);
        CemRuntime.renderFace(draw, face, scale);
        return true;
    }

    private static ModelBiped model() {
        if (renderer == null) renderer = new RenderPlayer();
        CemPlayers.register(renderer.modelBipedMain);
        return renderer.modelBipedMain;
    }

    private static CemResources.Selection select(Entity entity) {
        if (!(entity instanceof AbstractClientPlayer)) return null;
        AbstractClientPlayer player = (AbstractClientPlayer) entity;
        ModelBiped model = model();
        return select(player, model, CemRuntime.bindingFor(model, player), player.getLocationSkin());
    }

    private static CemResources.Selection select(AbstractClientPlayer player, ModelBiped model, CemBinding binding,
        ResourceLocation skin) {
        if (!CemConfig.enabled || !CemResources.INSTANCE.hasModels() || CemAnimationControl.vanilla(player))
            return null;
        ResourceLocation previous = CemRuntime.texture();
        try {
            CemRuntime.texture(skin);
            return CemResources.INSTANCE.select(CemTargets.candidates(binding, player, model, skin), binding, player);
        } finally {
            CemRuntime.texture(previous);
        }
    }

    static Face face(CemModel model) {
        Face face = FACES.get(model);
        if (face == null) {
            face = analyze(model);
            FACES.put(model, face);
            if (face.coverage == Coverage.FACE) Salamander.LOG
                .info("The CEM player model '{}' only adds a face: mods drawing the body keep it", model.source);
            else Salamander.LOG.debug("The CEM player model '{}' covers the body: {}", model.source, face.reason);
        }
        return face;
    }

    /**
     * Whether everything the model changes or animates is on the head, beside the vanilla head and hat: then it's a
     * face. A part that keeps vanilla's boxes is the same; any box, texture, sprite or turn of the parts' own that
     * vanilla's player doesn't have, anywhere but the head, covers the body, as does an animation of anything but the
     * face and the head's own turn.
     */
    static Face analyze(CemModel model) {
        List<CemModel.Node> roots = new ArrayList<>();
        Set<CemModel.Node> skipped = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<CemModel.Node> face = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<CemModel.Node> originals = Collections.newSetFromMap(new IdentityHashMap<>());
        originals.addAll(model.originalParts.values());
        CemModel.Node head = model.originalParts.get("head"), headwear = model.originalParts.get("headwear");
        if (head == null) return new Face(Coverage.BODY, roots, skipped, "it has no head");
        for (Map.Entry<String, CemModel.Node> entry : model.originalParts.entrySet()) {
            String name = entry.getKey();
            CemModel.Node part = entry.getValue();
            List<CemModel.Node> custom = new ArrayList<>();
            for (CemModel.Node child : part.children) if (!originals.contains(child)) custom.add(child);
            if (HEAD.contains(name)) {
                if (name.endsWith("_ear")) {
                    // An ear hangs from the head with its own pose: it draws whole, its transform included.
                    if (!custom.isEmpty()) {
                        roots.add(part);
                        collect(part, face);
                    }
                    continue;
                }
                boolean cube = false;
                for (CemModel.Node child : custom) {
                    roots.add(child);
                    collect(child, face);
                    cube |= markCubes(child, new float[3], name, skipped);
                }
                // A head replaced without vanilla's cube has another shape: the pack draws it.
                if (name.equals("head") && !part.vanillaGeometry && !cube)
                    return new Face(Coverage.BODY, roots, skipped, "it reshapes the head");
                continue;
            }
            if (custom.isEmpty()) {
                if (part.vanillaGeometry || name.startsWith("$") || !VANILLA.containsKey(name)) continue;
                // Replaced by nothing: the part is gone.
                return new Face(Coverage.BODY, roots, skipped, "it removes '" + name + "'");
            }
            List<float[]> boxes = new ArrayList<>();
            for (CemModel.Node child : custom) if (!flatten(child, new float[3], boxes))
                return new Face(Coverage.BODY, roots, skipped, "it turns, scales or retextures '" + name + "'");
            float[] vanilla = VANILLA.get(name);
            boolean same = vanilla != null && !part.vanillaGeometry
                && boxes.size() == 1
                && matches(boxes.get(0), vanilla, name.equals("cloak"));
            if (!same && !boxes.isEmpty()) return new Face(Coverage.BODY, roots, skipped, "it changes '" + name + "'");
            if (!same && !part.vanillaGeometry && vanilla != null)
                return new Face(Coverage.BODY, roots, skipped, "it removes '" + name + "'");
        }
        for (CemModel.Node node : model.nodes) {
            if (!model.reaches(node) || face.contains(node)) continue;
            for (int property = 0; property < CemModel.PROPERTIES.size(); property++) {
                if (!model.animates(node, property)) continue;
                // The head's own turn: the host turns its head itself.
                boolean look = (node == head || node == headwear) && property >= 3 && property <= 5;
                if (!look) return new Face(
                    Coverage.BODY,
                    roots,
                    skipped,
                    "it animates '" + node.id + "." + CemModel.PROPERTIES.get(property) + "'");
            }
        }
        return new Face(Coverage.FACE, roots, skipped, null);
    }

    private static void collect(CemModel.Node node, Set<CemModel.Node> into) {
        into.add(node);
        for (CemModel.Node child : node.children) collect(child, into);
    }

    /**
     * Marks the nodes under the head whose own boxes are all the vanilla head's or hat's cube, placed as vanilla
     * places it: the host draws those. Whether the head's cube was among them.
     */
    private static boolean markCubes(CemModel.Node node, float[] at, String part, Set<CemModel.Node> skipped) {
        if (!plain(node)) return false;
        float[] here = { at[0] + (float) node.transform[0], at[1] + (float) node.transform[1],
            at[2] + (float) node.transform[2] };
        boolean head = false;
        if (!node.boxes.isEmpty() && node.sprites.isEmpty() && node.texture == null) {
            boolean all = true;
            for (CemModel.Box box : node.boxes) {
                float[] placed = place(box, here);
                boolean isHead = matches(placed, VANILLA.get("head"), false);
                all &= isHead || matches(placed, VANILLA.get("headwear"), false);
                head |= isHead;
            }
            if (all) skipped.add(node);
            else head = false;
        }
        for (CemModel.Node child : node.children) head |= markCubes(child, here, part, skipped);
        return head;
    }

    /** Adds the node's boxes and its children's, placed in the part's frame; false for anything but plain boxes. */
    private static boolean flatten(CemModel.Node node, float[] at, List<float[]> boxes) {
        if (!node.sprites.isEmpty() || node.texture != null) return false;
        boolean geometry = hasGeometry(node);
        if (geometry && !plain(node)) return false;
        float[] here = { at[0] + (float) node.transform[0], at[1] + (float) node.transform[1],
            at[2] + (float) node.transform[2] };
        for (CemModel.Box box : node.boxes) {
            float[] placed = place(box, here);
            if (Float.isNaN(placed[6])) return false;
            boxes.add(placed);
        }
        for (CemModel.Node child : node.children) if (!flatten(child, here, boxes)) return false;
        return true;
    }

    private static boolean hasGeometry(CemModel.Node node) {
        if (!node.boxes.isEmpty() || !node.sprites.isEmpty()) return true;
        for (CemModel.Node child : node.children) if (hasGeometry(child)) return true;
        return false;
    }

    /** No turn and no scale of its own. */
    private static boolean plain(CemModel.Node node) {
        double[] t = node.transform;
        return Math.abs(t[3]) < EPSILON && Math.abs(t[4]) < EPSILON
            && Math.abs(t[5]) < EPSILON
            && Math.abs(t[6] - 1) < EPSILON
            && Math.abs(t[7] - 1) < EPSILON
            && Math.abs(t[8] - 1) < EPSILON;
    }

    /** x, y, z, width, height, depth, u, v, inflation (or NaN when uneven), texture width and height. */
    private static float[] place(CemModel.Box box, float[] at) {
        float[] c = box.coordinates;
        float inflation = box.inflation[0] == box.inflation[1] && box.inflation[1] == box.inflation[2]
            ? box.inflation[0]
            : Float.NaN;
        float[] uv = box.mirrorU || box.mirrorV ? null : box.faceUvs == null ? box.uv : offset(box);
        return new float[] { at[0] + c[0], at[1] + c[1], at[2] + c[2], c[3], c[4], c[5], uv == null ? Float.NaN : uv[0],
            uv == null ? Float.NaN : uv[1], inflation, box.textureWidth, box.textureHeight };
    }

    /**
     * The texture offset of a box whose six face UVs, as Blockbench writes them, lay out its faces as a texture
     * offset would; null for any other layout.
     */
    private static float[] offset(CemModel.Box box) {
        float[][] faces = box.faceUvs;
        float w = box.coordinates[3], h = box.coordinates[4], d = box.coordinates[5];
        if (faces[4] == null) return null;
        float u = faces[4][0] - d, v = faces[4][1] - d;
        // West, east, up, down, north, south, as the loader orders them.
        float[][] standard = { { u + d + w, v + d, u + 2 * d + w, v + d + h }, { u, v + d, u + d, v + d + h },
            { u + d + w, v + d, u + d, v }, { u + d + 2 * w, v, u + d + w, v + d },
            { u + d, v + d, u + d + w, v + d + h }, { u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h } };
        for (int face = 0; face < 6; face++) {
            if (faces[face] == null) return null;
            for (int i = 0; i < 4; i++) if (Math.abs(faces[face][i] - standard[face][i]) > EPSILON) return null;
        }
        return new float[] { u, v };
    }

    private static boolean matches(float[] box, float[] vanilla, boolean anyTextureSize) {
        if (vanilla == null) return false;
        if (!anyTextureSize && (box[9] != 64 || box[10] != 64)) return false;
        for (int i = 1; i < 9; i++) if (i != 3 && !(Math.abs(box[i] - vanilla[i]) < EPSILON)) return false;
        if (Math.abs(box[0] - vanilla[0]) < EPSILON && Math.abs(box[3] - vanilla[3]) < EPSILON) return true;
        // An arm or sleeve may be slim: one narrower, on the side away from the body.
        return !Float.isNaN(vanilla[9]) && Math.abs(box[0] - vanilla[9]) < EPSILON
            && Math.abs(box[3] - (vanilla[3] - 1)) < EPSILON;
    }
}
