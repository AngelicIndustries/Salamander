package industries.angelic.salamander.cem.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.util.ResourceLocation;

import industries.angelic.salamander.cem.animation.CemExpression;
import industries.angelic.salamander.cem.animation.CemExpressionParser;
import industries.angelic.salamander.cem.animation.CemVariables;

/** Compiled geometry and assignments, shared by independent per-entity instances. */
public final class CemModel {

    public static final int STRIDE = 11;
    public static final List<String> PROPERTIES = Collections.unmodifiableList(
        Arrays.asList("tx", "ty", "tz", "rx", "ry", "rz", "sx", "sy", "sz", "visible", "visible_boxes"));
    public static final Set<String> INPUTS = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "time",
                "day_time",
                "day_count",
                "limb_swing",
                "limb_speed",
                "age",
                "head_pitch",
                "head_yaw",
                "player_pos_x",
                "player_pos_y",
                "player_pos_z",
                "player_rot_x",
                "player_rot_y",
                "frame_time",
                "frame_counter",
                "dimension",
                "distance",
                "rule_index",
                "health",
                "hurt_time",
                "death_time",
                "anger_time",
                "anger_time_start",
                "max_health",
                "pos_x",
                "pos_y",
                "pos_z",
                "rot_x",
                "rot_y",
                "swing_progress",
                "move_forward",
                "move_strafing",
                "is_climbing",
                "is_crawling",
                "is_swimming",
                "is_gliding",
                "is_first_person_hand",
                "id",
                "is_aggressive",
                "is_alive",
                "is_burning",
                "is_child",
                "is_glowing",
                "is_hurt",
                "is_in_hand",
                "is_in_item_frame",
                "is_in_ground",
                "is_in_gui",
                "is_in_lava",
                "is_in_water",
                "is_invisible",
                "is_on_ground",
                "is_on_head",
                "is_on_shoulder",
                "is_ridden",
                "is_riding",
                "is_sitting",
                "is_sneaking",
                "is_sprinting",
                "is_tamed",
                "is_wet",
                // EMF additions
                "is_paused",
                "is_jumping",
                "is_blocking",
                "is_using_item",
                "is_right_handed",
                "is_holding_item_right",
                "is_holding_item_left",
                "is_swinging_right_arm",
                "is_swinging_left_arm",
                "fluid_depth",
                "fluid_depth_up",
                "fluid_depth_down")));
    public static final Set<String> RENDER_PROPERTIES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "render.shadow_size",
                "render.shadow_opacity",
                "render.shadow_offset_x",
                "render.shadow_offset_z",
                "render.leash_offset_x",
                "render.leash_offset_y",
                "render.leash_offset_z")));
    /** Render properties by slot. */
    private static final List<String> RENDER_NAMES = Collections
        .unmodifiableList(new ArrayList<>(new java.util.TreeSet<>(RENDER_PROPERTIES)));

    public final ResourceLocation source;
    public final ResourceLocation texture;
    public final float shadowSize;
    public final Node root;
    public final List<Node> nodes;
    public final Map<String, Node> originalParts;
    private final List<Assignment> program = new ArrayList<>();
    /** The inputs this model reads, by slot: each is looked up once per evaluation, not once per use. */
    private final List<String> inputNames = new ArrayList<>();
    private final Map<String, Integer> inputSlots = new HashMap<>();
    private final double[] defaults;
    private final boolean[] animated;
    /** By node index: whether the node hangs from {@link #root}. A replaced parent drops some of its children. */
    private final boolean[] reachable;

    public CemModel(ResourceLocation source, ResourceLocation texture, float shadowSize, Node root, List<Node> nodes,
        Map<String, Node> originalParts, List<Animation> animations) {
        this.source = source;
        this.texture = texture;
        this.shadowSize = shadowSize;
        this.root = root;
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.originalParts = Collections.unmodifiableMap(new LinkedHashMap<>(originalParts));
        this.defaults = new double[nodes.size() * STRIDE];
        this.animated = new boolean[defaults.length];
        for (Node node : nodes) System.arraycopy(node.transform, 0, defaults, node.index * STRIDE, STRIDE);
        this.reachable = new boolean[nodes.size()];
        mark(root);
        for (Animation animation : animations) {
            Access target = access(animation.target, animation.owner, animation.part, true);
            CemExpression expression = CemExpressionParser
                .compile(animation.expression, name -> access(name, animation.owner, animation.part, false).reader());
            program.add(new Assignment(target, expression));
            if (target.kind == Access.POSE) animated[target.index] = true;
        }
    }

    public int assignmentCount() {
        return program.size();
    }

    public Instance newInstance() {
        return new Instance(new CemVariables());
    }

    /** An instance sharing an entity's variables with its other models. */
    public Instance newInstance(CemVariables variables) {
        return new Instance(variables);
    }

    public Node find(String path, Node self, Node part) {
        String[] segments = path.split(":", -1);
        Node found = segments[0].equals("this") ? self
            : segments[0].equals("part") ? part : originalParts.get(segments[0]);
        if (found == null) found = findChild(root, segments[0], true);
        if (found == null) for (Node candidate : nodes) if (segments[0].equals(candidate.id)) {
            found = candidate;
            break;
        }
        for (int i = 1; found != null && i < segments.length; i++) found = findChild(found, segments[i], false);
        return found;
    }

    private static Node findChild(Node node, String name, boolean includeSelf) {
        if (includeSelf && name.equals(node.id)) return node;
        for (Node child : node.children) {
            Node found = findChild(child, name, true);
            if (found != null) return found;
        }
        return null;
    }

    private Access access(String name, Node self, Node part, boolean writable) {
        int render = RENDER_NAMES.indexOf(name);
        if (render >= 0) return new Access(Access.RENDER, render, false);
        if (name.startsWith("var.") || name.startsWith("varb."))
            return new Access(Access.VARIABLE, CemVariables.slot(name), name.startsWith("varb."));
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            Node node = find(name.substring(0, dot), self, part);
            int property = PROPERTIES.indexOf(name.substring(dot + 1));
            if (node != null && property >= 0)
                return new Access(Access.POSE, node.index * STRIDE + property, property >= 9);
        }
        if (!writable && INPUTS.contains(name)) {
            Integer slot = inputSlots.get(name);
            if (slot == null) {
                slot = inputNames.size();
                inputNames.add(name);
                inputSlots.put(name, slot);
            }
            return new Access(Access.INPUT, slot, false);
        }
        throw new IllegalArgumentException(
            source + ": unknown " + (writable ? "animation target" : "variable") + " '" + name + "'");
    }

    public final class Instance {

        public final double[] pose = defaults.clone();
        public final CemVariables variables;
        public final Map<String, Double> renderProperties = new HashMap<>();
        private final boolean[] hidden = new boolean[nodes.size()];
        private final double[] inputValues = new double[inputNames.size()];
        private final double[] renderValues = new double[RENDER_NAMES.size()];
        private long lastFrame = Long.MIN_VALUE;

        private Instance(CemVariables variables) {
            this.variables = variables;
        }

        public void nativePose(Node node, float[] transform, boolean visible) {
            int offset = node.index * STRIDE;
            for (int i = 0; i < 6; i++) if (!animated[offset + i]) pose[offset + i] = transform[i];
            if (!animated[offset + 9]) pose[offset + 9] = visible && !hidden[node.index] ? 1 : 0;
        }

        /** Match a native follower to its animated bone without overwriting explicit follower animations. */
        public void followPose(String source, String target) {
            followPose(source, target, false);
        }

        public void followPose(String source, String target, boolean force) {
            Node from = originalParts.get(source), to = originalParts.get(target);
            if (from == null || to == null) return;
            int fromOffset = from.index * STRIDE, toOffset = to.index * STRIDE;
            for (int i = 0; i < 9; i++) if (force || !animated[toOffset + i]) pose[toOffset + i] = pose[fromOffset + i];
        }

        public boolean evaluated(long frame) {
            return lastFrame == frame;
        }

        /** First-person hands use the pack geometry with the native hand pose, without body animations. */
        public void staticPose(Consumer<double[]> vanillaPose) {
            System.arraycopy(defaults, 0, pose, 0, defaults.length);
            vanillaPose.accept(pose);
        }

        /** Reuses the evaluated pose across eye, hurt, and shader passes in the same client frame. */
        public void evaluate(long frame, Map<String, Double> inputs, Consumer<double[]> vanillaPose) {
            if (lastFrame == frame) return;
            java.util.Arrays.fill(hidden, false);
            System.arraycopy(defaults, 0, pose, 0, defaults.length);
            vanillaPose.accept(pose);
            for (int i = 0; i < inputValues.length; i++) {
                Double value = inputs.get(inputNames.get(i));
                inputValues[i] = value == null ? 0 : value;
            }
            for (int i = 0; i < renderValues.length; i++) {
                String property = RENDER_NAMES.get(i);
                Double value = inputs.get(property);
                renderValues[i] = value != null ? value
                    : property.equals("render.shadow_size") ? shadowSize
                        : property.equals("render.shadow_opacity") ? 1 : 0;
            }
            CemExpression.Context context = new CemExpression.Context(
                pose,
                variables,
                inputs,
                renderProperties,
                inputValues,
                renderValues);
            for (Assignment assignment : program) {
                double value = assignment.expression.evaluate(context);
                assignment.target.set(context, value);
            }
            for (int i = 0; i < renderValues.length; i++) renderProperties.put(RENDER_NAMES.get(i), renderValues[i]);
            // Some standard packs intentionally divide by zero to hide a part. Never pass infinity/NaN to GL.
            for (Node node : nodes) {
                int offset = node.index * STRIDE;
                for (int i = 0; i < 9; i++) if (!Double.isFinite(pose[offset + i])) {
                    pose[offset + i] = defaults[offset + i];
                    pose[offset + 9] = 0;
                    hidden[node.index] = true;
                }
            }
            lastFrame = frame;
        }
    }

    private void mark(Node node) {
        if (node.index >= 0 && node.index < reachable.length) reachable[node.index] = true;
        for (Node child : node.children) mark(child);
    }

    /** Whether {@code node} is one of this model's and hangs from its root. */
    public boolean reaches(Node node) {
        return node != null && node.index >= 0
            && node.index < reachable.length
            && reachable[node.index]
            && nodes.get(node.index) == node;
    }

    public static final class Node {

        public final int index;
        public final String id;
        public final String vanillaPart;
        public final double[] transform = { 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1 };
        public final List<Node> children = new ArrayList<>();
        public final List<Box> boxes = new ArrayList<>();
        public final List<Box> sprites = new ArrayList<>();
        public final Map<String, float[]> attachments = new LinkedHashMap<>();
        public ResourceLocation texture;
        public boolean vanillaGeometry;

        public Node(int index, String id, String vanillaPart) {
            this.index = index;
            this.id = id;
            this.vanillaPart = vanillaPart;
            this.vanillaGeometry = vanillaPart != null;
        }
    }

    /** Model-space box in Minecraft coordinates; UV dimensions remain uninflated. */
    public static final class Box {

        public final float[] coordinates;
        public final float[] inflation;
        public final float[] uv;
        public final int textureWidth;
        public final int textureHeight;
        public final boolean mirrorU;
        public final boolean mirrorV;
        /** East, west, down, up, north, south; null entries omit the corresponding face. */
        public final float[][] faceUvs;

        public Box(float[] coordinates, float[] inflation, float[] uv, int textureWidth, int textureHeight,
            boolean mirrorU, boolean mirrorV) {
            this(coordinates, inflation, uv, textureWidth, textureHeight, mirrorU, mirrorV, null);
        }

        public Box(float[] coordinates, float[] inflation, float[] uv, int textureWidth, int textureHeight,
            boolean mirrorU, boolean mirrorV, float[][] faceUvs) {
            this.coordinates = coordinates;
            this.inflation = inflation;
            this.uv = uv;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.mirrorU = mirrorU;
            this.mirrorV = mirrorV;
            this.faceUvs = faceUvs;
        }
    }

    public static final class Animation {

        public final Node owner;
        public final Node part;
        public final String target;
        public final String expression;

        public Animation(Node owner, Node part, String target, String expression) {
            this.owner = owner;
            this.part = part;
            this.target = target;
            this.expression = expression;
        }
    }

    /** A name resolved when the model compiles: evaluation only indexes arrays. */
    private static final class Access {

        static final int POSE = 0, VARIABLE = 1, INPUT = 2, RENDER = 3;

        final int kind;
        final int index;
        final boolean bool;

        Access(int kind, int index, boolean bool) {
            this.kind = kind;
            this.index = index;
            this.bool = bool;
        }

        CemExpression reader() {
            int slot = index;
            switch (kind) {
                case POSE:
                    return c -> c.pose[slot];
                case VARIABLE:
                    return c -> c.slots.get(slot);
                case INPUT:
                    return c -> c.inputValues[slot];
                default:
                    return c -> c.renderValues[slot];
            }
        }

        void set(CemExpression.Context context, double value) {
            if (bool) value = CemExpressionParser.truth(value) ? 1 : 0;
            switch (kind) {
                case POSE:
                    context.pose[index] = value;
                    break;
                case VARIABLE:
                    context.slots.set(index, value);
                    break;
                case RENDER:
                    context.renderValues[index] = value;
                    break;
                default:
                    throw new IllegalStateException("CEM inputs are read-only");
            }
        }
    }

    private static final class Assignment {

        final Access target;
        final CemExpression expression;

        Assignment(Access target, CemExpression expression) {
            this.target = target;
            this.expression = expression;
        }
    }
}
