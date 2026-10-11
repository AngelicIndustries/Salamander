package org.fentanylsolutions.salamander.cem.loading;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.fentanylsolutions.salamander.cem.model.CemModel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Resolves CEM parts through a resource manager supplied by the caller, including lower-priority packs. */
public final class CemLoader {

    @FunctionalInterface
    public interface Resources {

        Reader open(ResourceLocation location) throws IOException;
    }

    private final Resources resources;
    private int resolutionDepth;
    private final List<CemModel.Node> nodes = new ArrayList<>();
    private final List<CemModel.Animation> animations = new ArrayList<>();
    private final Map<String, CemModel.Node> originals = new LinkedHashMap<>();
    private final List<ResourceLocation> missing = new ArrayList<>();

    public CemLoader(Resources resources) {
        this.resources = resources;
    }

    /** The adapter provides named vanilla pivots. The root is at the model origin. Use a new loader per model. */
    public CemModel load(ResourceLocation location, Map<String, float[]> pivots) throws IOException {
        return load(location, pivots, java.util.Collections.emptyMap());
    }

    public CemModel load(ResourceLocation location, Map<String, float[]> pivots, Map<String, String> parents)
        throws IOException {
        return load(location, pivots, parents, 64, 32);
    }

    public CemModel load(ResourceLocation location, Map<String, float[]> pivots, Map<String, String> parents, int width,
        int height) throws IOException {
        if (!nodes.isEmpty()) throw new IllegalStateException("CEM loader is single-use");
        JsonObject document = read(location);
        int[] textureSize = textureSize(document, new int[] { width, height });
        CemModel.Node root = node("root", "root");
        originals.put("root", root);
        for (Map.Entry<String, float[]> entry : pivots.entrySet()) {
            if (entry.getKey()
                .equals("root")) continue;
            CemModel.Node part = node(entry.getKey(), entry.getKey());
            for (int axis = 0; axis < Math.min(9, entry.getValue().length); axis++)
                part.transform[axis] = entry.getValue()[axis];
            originals.put(entry.getKey(), part);
        }
        for (Map.Entry<String, CemModel.Node> entry : originals.entrySet()) {
            if (entry.getValue() == root) continue;
            CemModel.Node parent = originals.get(parents.getOrDefault(entry.getKey(), "root"));
            if (parent == null || parent == entry.getValue())
                throw new IllegalArgumentException("Invalid native CEM parent: " + entry.getKey());
            parent.children.add(entry.getValue());
        }
        Map<String, JsonObject> bases = new LinkedHashMap<>();
        for (JsonElement element : document.getAsJsonArray("models")) {
            JsonObject model = copy(element.getAsJsonObject());
            if (model.has("baseId")) {
                String id = model.get("baseId")
                    .getAsString();
                JsonObject base = bases.get(id);
                if (base == null) throw new IllegalArgumentException(location + ": missing baseId '" + id + "'");
                inherit(model, base, true);
            }
            model = resolve(model, location, new ArrayDeque<>());
            String partName = model.get("part")
                .getAsString();
            CemModel.Node parent = originals.get(partName);
            if (parent == null)
                throw new IllegalArgumentException(location + ": unsupported model part '" + partName + "'");
            if (!model.has("attach") || !model.get("attach")
                .getAsBoolean()) {
                parent.vanillaGeometry = false;
                // Named native children have their own CEM slots (villager nose, bee torso, etc.).
                // Replacing a parent removes its geometry and anonymous children, not those slots.
                // Several entries can replace the same slot. Preserve custom siblings already loaded.
                parent.children.removeIf(
                    child -> child.vanillaPart != null && (parent == root || child.vanillaPart.startsWith("$")));
            }
            CemModel.Node custom = parsePart(model, parent, textureSize, location, 0);
            parent.children.add(custom);
            if (model.has("id")) bases.put(
                model.get("id")
                    .getAsString(),
                model);
        }
        ResourceLocation texture = document.has("texture") ? path(
            location,
            document.get("texture")
                .getAsString(),
            ".png") : null;
        float shadow = document.has("shadowSize") ? document.get("shadowSize")
            .getAsFloat() : -1;
        if (!Float.isFinite(shadow) || shadow < -1)
            throw new IllegalArgumentException(location + ": invalid shadow size");
        return new CemModel(location, texture, shadow, root, nodes, originals, animations);
    }

    private CemModel.Node parsePart(JsonObject object, CemModel.Node original, int[] inheritedSize,
        ResourceLocation source, int depth) throws IOException {
        if (depth > 64 || nodes.size() > 16384)
            throw new IllegalArgumentException(source + ": excessive model nesting/parts");
        object = resolve(object, source, new ArrayDeque<>());
        CemModel.Node part = node(
            object.has("id") ? object.get("id")
                .getAsString() : "",
            null);
        if (object.has("texture")) part.texture = path(
            source,
            object.get("texture")
                .getAsString(),
            ".png");
        int[] size = textureSize(object, inheritedSize);
        String invert = object.has("invertAxis") ? object.get("invertAxis")
            .getAsString() : "";
        float[] translate = vector(object, "translate", 3, new float[3]);
        float[] rotate = vector(object, "rotate", 3, new float[3]);
        for (int axis = 0; axis < 3; axis++) {
            float sign = invert.indexOf("xyz".charAt(axis)) >= 0 ? -1 : 1;
            part.transform[axis] = translate[axis] * sign;
            part.transform[axis + 3] = Math.toRadians(rotate[axis] * sign);
        }
        if (object.has("attachments"))
            for (Map.Entry<String, JsonElement> attachment : object.getAsJsonObject("attachments")
                .entrySet()) {
                    if (!attachment.getKey()
                        .equals("left_handheld_item")
                        && !attachment.getKey()
                            .equals("right_handheld_item"))
                        throw new IllegalArgumentException(source + ": unknown attachment " + attachment.getKey());
                    float[] point = vector(object.getAsJsonObject("attachments"), attachment.getKey(), 3, null);
                    for (int axis = 0; axis < 3; axis++)
                        if (invert.indexOf("xyz".charAt(axis)) >= 0) point[axis] = -point[axis];
                    part.attachments.put(attachment.getKey(), point);
                }
        if (object.has("scale")) {
            double scale = object.get("scale")
                .getAsDouble();
            if (!Double.isFinite(scale)) throw new IllegalArgumentException(source + ": invalid scale");
            part.transform[6] = part.transform[7] = part.transform[8] = scale;
        }
        String mirror = object.has("mirrorTexture") ? object.get("mirrorTexture")
            .getAsString() : "";
        if (object.has("boxes")) for (JsonElement element : object.getAsJsonArray("boxes")) {
            JsonObject box = element.getAsJsonObject();
            float[] coordinates = vector(box, "coordinates", 6, null);
            for (int axis = 0; axis < 3; axis++) {
                if (invert.indexOf("xyz".charAt(axis)) >= 0)
                    coordinates[axis] = -coordinates[axis] - coordinates[axis + 3];
            }
            float add = box.has("sizeAdd") ? box.get("sizeAdd")
                .getAsFloat() : 0;
            float[] inflation = vector(box, "sizesAdd", 3, new float[] { add, add, add });
            float[] uv = vector(box, "textureOffset", 2, new float[2]);
            float[][] faceUvs = null;
            if (!box.has("textureOffset")) {
                // CEM names follow the editor's axes: model-space +X is west and -Y is up.
                String[] names = { "uvWest", "uvEast", "uvUp", "uvDown", "uvNorth", "uvSouth" };
                String[] aliases = { "uvLeft", "uvRight", "uvUp", "uvDown", "uvFront", "uvBack" };
                faceUvs = new float[6][];
                boolean any = false;
                for (int face = 0; face < 6; face++) {
                    String key = box.has(names[face]) ? names[face] : aliases[face];
                    if (box.has(key)) {
                        faceUvs[face] = vector(box, key, 4, null);
                        any = true;
                    }
                }
                if (!any) throw new IllegalArgumentException(source + ": box has no textureOffset or face UVs");
            }
            part.boxes.add(
                new CemModel.Box(
                    coordinates,
                    inflation,
                    uv,
                    size[0],
                    size[1],
                    mirror.contains("u"),
                    mirror.contains("v"),
                    faceUvs));
        }
        if (object.has("sprites")) for (JsonElement element : object.getAsJsonArray("sprites")) {
            JsonObject sprite = element.getAsJsonObject();
            float[] c = vector(sprite, "coordinates", 6, null);
            for (int axis = 0; axis < 3; axis++) {
                if (c[axis + 3] < 0) throw new IllegalArgumentException(source + ": negative sprite dimensions");
                if (invert.indexOf("xyz".charAt(axis)) >= 0) c[axis] = -c[axis] - c[axis + 3];
            }
            float add = sprite.has("sizeAdd") ? sprite.get("sizeAdd")
                .getAsFloat() : 0;
            if (!Float.isFinite(add)) throw new IllegalArgumentException(source + ": invalid sprite inflation");
            part.sprites.add(
                new CemModel.Box(
                    c,
                    new float[] { add, add, add },
                    vector(sprite, "textureOffset", 2, null),
                    size[0],
                    size[1],
                    mirror.contains("u"),
                    mirror.contains("v")));
        }
        if (object.has("animations")) for (JsonElement element : object.getAsJsonArray("animations")) {
            for (Map.Entry<String, JsonElement> assignment : element.getAsJsonObject()
                .entrySet())
                animations.add(
                    new CemModel.Animation(
                        part,
                        original,
                        assignment.getKey(),
                        assignment.getValue()
                            .getAsString()));
        }
        if (object.has("submodel"))
            part.children.add(parsePart(object.getAsJsonObject("submodel"), original, size, source, depth + 1));
        if (object.has("submodels")) for (JsonElement child : object.getAsJsonArray("submodels"))
            part.children.add(parsePart(child.getAsJsonObject(), original, size, source, depth + 1));
        return part;
    }

    private JsonObject resolve(JsonObject object, ResourceLocation source, Deque<ResourceLocation> visiting)
        throws IOException {
        if (++resolutionDepth > 64) {
            --resolutionDepth;
            throw new IOException(source + ": excessive JPM nesting");
        }
        try {
            JsonObject result = copy(object);
            if (result.has("texture")) result.addProperty(
                "texture",
                path(
                    source,
                    result.get("texture")
                        .getAsString(),
                    ".png").toString());
            if (result.has("submodel"))
                result.add("submodel", resolve(result.getAsJsonObject("submodel"), source, visiting));
            if (result.has("submodels")) {
                JsonArray children = new JsonArray();
                for (JsonElement child : result.getAsJsonArray("submodels"))
                    children.add(resolve(child.getAsJsonObject(), source, visiting));
                result.add("submodels", children);
            }
            if (result.has("model")) {
                ResourceLocation reference = path(
                    source,
                    result.get("model")
                        .getAsString(),
                    ".jpm");
                if (visiting.contains(reference) || visiting.size() >= 64) throw new IOException(
                    source + ": circular/excessive JPM references: " + visiting + " -> " + reference);
                result.remove("model");
                JsonObject referenced = reference(reference);
                if (referenced != null) {
                    visiting.addLast(reference);
                    JsonObject external = resolve(referenced, reference, visiting);
                    visiting.removeLast();
                    inherit(result, external, false);
                }
            }
            return result;
        } finally {
            --resolutionDepth;
        }
    }

    /** The parts' {@code model} references that did not exist, in load order. */
    public List<ResourceLocation> missingReferences() {
        return java.util.Collections.unmodifiableList(missing);
    }

    /**
     * As in EMF, a part whose referenced model file does not exist keeps only what it declares itself, so a pack can
     * point at files that add-on packs provide. A file that exists but does not parse still fails the model.
     */
    private JsonObject reference(ResourceLocation location) throws IOException {
        try {
            return read(location);
        } catch (FileNotFoundException absent) {
            if (!missing.contains(location)) missing.add(location);
            return null;
        }
    }

    private JsonObject read(ResourceLocation location) throws IOException {
        try (Reader reader = resources.open(location)) {
            return new JsonParser().parse(reader)
                .getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid CEM resource " + location, exception);
        }
    }

    private CemModel.Node node(String id, String vanillaPart) {
        CemModel.Node node = new CemModel.Node(nodes.size(), id, vanillaPart);
        nodes.add(node);
        return node;
    }

    private static JsonObject copy(JsonObject object) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) result.add(entry.getKey(), entry.getValue());
        return result;
    }

    private static void inherit(JsonObject child, JsonObject parent, boolean excludeId) {
        for (Map.Entry<String, JsonElement> entry : parent.entrySet()) {
            if (!child.has(entry.getKey()) && !(excludeId && entry.getKey()
                .equals("id"))) child.add(entry.getKey(), entry.getValue());
        }
    }

    private static int[] textureSize(JsonObject object, int[] fallback) {
        float[] size = vector(object, "textureSize", 2, new float[] { fallback[0], fallback[1] });
        if (size[0] <= 0 || size[1] <= 0 || size[0] != (int) size[0] || size[1] != (int) size[1])
            throw new IllegalArgumentException("Invalid CEM texture size");
        return new int[] { (int) size[0], (int) size[1] };
    }

    private static float[] vector(JsonObject object, String name, int length, float[] fallback) {
        if (!object.has(name)) {
            if (fallback != null) return fallback;
            throw new IllegalArgumentException("Missing CEM '" + name + "'");
        }
        JsonArray array = object.getAsJsonArray(name);
        if (array.size() != length)
            throw new IllegalArgumentException("Expected " + length + " values in '" + name + "'");
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            result[i] = array.get(i)
                .getAsFloat();
            if (!Float.isFinite(result[i])) throw new IllegalArgumentException("Non-finite CEM '" + name + "'");
        }
        return result;
    }

    public static ResourceLocation path(ResourceLocation source, String reference, String extension) {
        String domain = source.getResourceDomain();
        String path = reference;
        int colon = reference.indexOf(':');
        if (colon >= 0) {
            domain = reference.substring(0, colon);
            path = reference.substring(colon + 1);
        } else if (reference.startsWith("~/")) path = "optifine/" + reference.substring(2);
        else if (reference.startsWith("./") || !reference.contains("/")) path = source.getResourcePath()
            .substring(
                0,
                source.getResourcePath()
                    .lastIndexOf('/') + 1)
            + (reference.startsWith("./") ? reference.substring(2) : reference);
        if (!path.endsWith(extension)) path += extension;
        Deque<String> segments = new ArrayDeque<>();
        for (String segment : path.split("/")) {
            if (segment.equals("..")) {
                if (segments.isEmpty())
                    throw new IllegalArgumentException("CEM path escapes resource domain: " + reference);
                segments.removeLast();
            } else if (!segment.isEmpty() && !segment.equals(".")) segments.addLast(segment);
        }
        return new ResourceLocation(domain, String.join("/", segments));
    }
}
