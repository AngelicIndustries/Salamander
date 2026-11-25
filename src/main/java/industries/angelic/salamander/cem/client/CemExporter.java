package industries.angelic.salamander.cem.client;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.mixins.early.minecraft.client.AccessorCemModelBox;

/**
 * Writes another mod's entity model as a .jem, as EMF's exporter does: its part names, pivots, boxes and texture
 * coordinates, ready for an editor and loading back as the very same model. Rotations stay out: the game's own
 * animation poses the parts.
 */
public final class CemExporter {

    private static final Set<String> EXPORTED = new HashSet<>();

    private CemExporter() {}

    /** Exports the model the first time this session it draws this subject's kind of entity. */
    public static void offer(Object subject, ModelBase model) {
        String target = CemTargets.target(subject);
        if (target == null || !EXPORTED.add(
            target + "|"
                + model.getClass()
                    .getName()))
            return;
        Map<String, ModelRenderer> parts = CemModded.parts(model, subject);
        if (parts == null) return;
        int colon = target.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : target.substring(0, colon), name = target.substring(colon + 1);
        File file = new File(
            net.minecraft.client.Minecraft.getMinecraft().mcDataDir,
            "salamander/cem-export/" + namespace + "/" + name + ".jem");
        try {
            JsonObject jem = jem(model, parts, box -> ((AccessorCemModelBox) box).salamander$quads());
            Files.createDirectories(
                file.getParentFile()
                    .toPath());
            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting()
                    .create()
                    .toJson(jem, writer);
            }
            Salamander.LOG.info(
                "Exported the CEM model '{}' of {} to {}, parts: {}",
                target,
                model.getClass()
                    .getName(),
                file,
                String.join(", ", partNames(parts)));
        } catch (IOException | RuntimeException exception) {
            Salamander.LOG.warn("Cannot export the CEM model '{}' to {}: {}", target, file, exception.toString());
        }
    }

    private static List<String> partNames(Map<String, ModelRenderer> parts) {
        List<String> names = new ArrayList<>();
        for (String name : parts.keySet()) if (!name.startsWith("$")) names.add(name);
        return names;
    }

    /**
     * The .jem of a model's named parts. {@code quads} gives a box's six faces as vanilla draws them; each part lists
     * its own boxes, its children being parts of their own.
     */
    public static JsonObject jem(ModelBase model, Map<String, ModelRenderer> parts,
        Function<ModelBox, TexturedQuad[]> quads) {
        CemBinding binding = new CemBinding(model, parts);
        JsonObject jem = new JsonObject();
        jem.addProperty(
            "credit",
            "Exported by Salamander from " + model.getClass()
                .getName());
        jem.add("textureSize", vector(binding.textureWidth, binding.textureHeight));
        JsonArray models = new JsonArray();
        for (Map.Entry<String, ModelRenderer> entry : binding.parts.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("$")) continue;
            models.add(part(name, entry.getValue(), binding, quads));
        }
        jem.add("models", models);
        return jem;
    }

    private static JsonObject part(String name, ModelRenderer part, CemBinding binding,
        Function<ModelBox, TexturedQuad[]> quads) {
        // CEM's axes are the editor's: x and y inverted, y measured up from the ground 24 pixels below the origin. A
        // child sits where its parents' pivots put it, so an editor shows it in place; the pack model's translate
        // cancels that offset again, so it loads back exactly.
        float x = part.rotationPointX, y = part.rotationPointY, z = part.rotationPointZ;
        for (String parent = binding.parents.get(name); parent != null; parent = binding.parents.get(parent)) {
            ModelRenderer above = binding.parts.get(parent);
            if (above == null) break;
            x += above.rotationPointX;
            y += above.rotationPointY;
            z += above.rotationPointZ;
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("part", name);
        entry.addProperty("id", name);
        entry.addProperty("invertAxis", "xy");
        entry.add("translate", vector(x, y - 24, -z));
        int[] size = null;
        JsonArray plain = new JsonArray(), mirrored = new JsonArray();
        if (part.cubeList != null) for (Object object : part.cubeList) {
            if (!(object instanceof ModelBox)) continue;
            ModelBox box = (ModelBox) object;
            TexturedQuad[] faces = quads.apply(box);
            if (faces == null || faces.length < 6) continue;
            float[] scale = textureScale(faces, part, binding);
            if (scale[0] != binding.textureWidth || scale[1] != binding.textureHeight)
                size = new int[] { (int) scale[0], (int) scale[1] };
            (mirrored(faces) ? mirrored : plain).add(box(box, faces, scale, x, y, z));
        }
        if (size != null) entry.add("textureSize", vector(size[0], size[1]));
        if (plain.size() > 0 || mirrored.size() == 0) entry.add("boxes", plain);
        if (mirrored.size() > 0) {
            if (plain.size() == 0) {
                entry.addProperty("mirrorTexture", "u");
                entry.add("boxes", mirrored);
            } else {
                JsonObject sub = new JsonObject();
                sub.addProperty("id", name + "_mirrored");
                sub.addProperty("invertAxis", "xy");
                sub.addProperty("mirrorTexture", "u");
                sub.add("boxes", mirrored);
                JsonArray submodels = new JsonArray();
                submodels.add(sub);
                entry.add("submodels", submodels);
            }
        }
        return entry;
    }

    private static JsonObject box(ModelBox box, TexturedQuad[] faces, float[] scale, float x, float y, float z) {
        int width = Math.round(box.posX2 - box.posX1), height = Math.round(box.posY2 - box.posY1),
            depth = Math.round(box.posZ2 - box.posZ1);
        double lowest = Double.MAX_VALUE;
        for (TexturedQuad face : faces)
            for (PositionTextureVertex vertex : face.vertexPositions) lowest = Math.min(lowest, vertex.vector3D.xCoord);
        float inflation = (float) (box.posX1 - lowest);
        JsonObject result = new JsonObject();
        result.add("coordinates", vector(-(x + box.posX2), 24 - (y + box.posY2), z + box.posZ1, width, height, depth));
        result.add(
            "textureOffset",
            vector(Math.round(lowest(faces[1], true) * scale[0]), Math.round(lowest(faces[2], false) * scale[1])));
        if (Math.abs(inflation) > 1e-5) result.addProperty("sizeAdd", round(inflation));
        return result;
    }

    /** A mirrored box swaps its two side faces. */
    private static boolean mirrored(TexturedQuad[] faces) {
        return faces[0].vertexPositions[0].vector3D.xCoord < faces[1].vertexPositions[0].vector3D.xCoord;
    }

    /**
     * The texture size the box's faces were mapped with: the part's, unless the model set it after adding the box,
     * which leaves vanilla's 64 by 32 in those faces.
     */
    private static float[] textureScale(TexturedQuad[] faces, ModelRenderer part, CemBinding binding) {
        float[][] options = { { part.textureWidth, part.textureHeight },
            { binding.textureWidth, binding.textureHeight }, { 64, 32 } };
        for (float[] option : options)
            if (whole(lowest(faces[1], true) * option[0]) && whole(lowest(faces[2], false) * option[1])
                && whole(highest(faces[0], true) * option[0])
                && whole(highest(faces[1], false) * option[1])) return option;
        return options[0];
    }

    private static boolean whole(double value) {
        return Math.abs(value - Math.rint(value)) < 1e-3;
    }

    private static double lowest(TexturedQuad face, boolean u) {
        double value = Double.MAX_VALUE;
        for (PositionTextureVertex vertex : face.vertexPositions)
            value = Math.min(value, u ? vertex.texturePositionX : vertex.texturePositionY);
        return value;
    }

    private static double highest(TexturedQuad face, boolean u) {
        double value = -Double.MAX_VALUE;
        for (PositionTextureVertex vertex : face.vertexPositions)
            value = Math.max(value, u ? vertex.texturePositionX : vertex.texturePositionY);
        return value;
    }

    private static JsonArray vector(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(new JsonPrimitive(round(value)));
        return array;
    }

    /** Four decimals, and whole numbers without any: pivots such as 1.9 read back as written. */
    private static Number round(double value) {
        double rounded = Math.round(value * 10000) / 10000.0;
        if (rounded == Math.rint(rounded) && Math.abs(rounded) < 1e9) return (long) rounded;
        return rounded;
    }
}
