package industries.angelic.salamander.cem.client;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import com.google.gson.JsonObject;

import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemBoxMesh;
import industries.angelic.salamander.cem.model.CemModel;

public class CemModdedTest {

    /** A mod's model as they come: named fields, an array, a mixed part, a child held by no field. */
    static final class Creature extends ModelBase {

        final ModelRenderer body;
        final ModelRenderer headPiece;
        final ModelRenderer[] legs = new ModelRenderer[2];
        final ModelRenderer tail;

        Creature() {
            body = new ModelRenderer(this, 0, 0);
            body.addBox(-4, -6, -3, 8, 12, 6);
            body.setRotationPoint(0, 10, 1);
            headPiece = new ModelRenderer(this, 28, 0);
            headPiece.addBox(-3, -3, -5, 6, 6, 5, 0.5F);
            headPiece.setRotationPoint(0, 4, -3);
            ModelRenderer ear = new ModelRenderer(this, 50, 0);
            ear.mirror = true;
            ear.addBox(-1, -2, -0.5F, 2, 2, 1);
            ear.setRotationPoint(2, -3, 0);
            headPiece.addChild(ear);
            for (int i = 0; i < legs.length; i++) {
                legs[i] = new ModelRenderer(this, 0, 18);
                legs[i].mirror = i == 1;
                legs[i].addBox(-1, 0, -1, 2, 6, 2);
                legs[i].setRotationPoint(i == 0 ? -1.9F : 1.9F, 16, 0);
            }
            tail = new ModelRenderer(this, 40, 20);
            tail.addBox(-1, 0, 0, 2, 2, 4);
            tail.mirror = true;
            tail.addBox(-1, 2, 0, 2, 2, 4, 0.25F);
            tail.setRotationPoint(0, 8, 4);
            body.addChild(tail);
        }
    }

    @Test
    public void namesReadAsPacksSpellThem() {
        assertEquals("wild_boar", CemModded.name("WildBoar"));
        assertEquals("tf_kobold", CemModded.name("TFKobold"));
        assertEquals("mob_ghoul", CemModded.name("mob.Ghoul"));
        assertEquals("left_wing2", CemModded.name("leftWing2"));
        assertEquals("deer", CemModded.name("Deer "));
    }

    @Test
    public void partsAreNamedAfterFieldsAndTheirPlaceInTheModel() {
        Creature model = new Creature();
        Map<String, ModelRenderer> parts = CemModded.discover(model);
        assertEquals(
            Arrays.asList("body", "head_piece", "legs1", "legs2", "tail", "part3"),
            new ArrayList<>(parts.keySet()));
        assertSame(model.legs[1], parts.get("legs2"));
        assertSame(model.headPiece.childModels.get(0), parts.get("part3"));
    }

    @Test
    public void anExportedModelLoadsBackAsTheSameGeometry() throws Exception {
        Creature model = new Creature();
        Map<String, ModelRenderer> parts = CemModded.discover(model);
        Field quadList = ModelBox.class.getDeclaredField("quadList");
        quadList.setAccessible(true);
        JsonObject jem = CemExporter.jem(model, parts, box -> {
            try {
                return (TexturedQuad[]) quadList.get(box);
            } catch (IllegalAccessException exception) {
                throw new AssertionError(exception);
            }
        });
        CemBinding binding = new CemBinding(model, parts);
        CemModel loaded = new CemLoader(location -> new StringReader(jem.toString())).load(
            new ResourceLocation("test:optifine/cem/creature.jem"),
            binding.pivots,
            binding.parents,
            binding.textureWidth,
            binding.textureHeight);
        assertTrue(loaded.warnings.toString(), loaded.warnings.isEmpty());
        for (Map.Entry<String, ModelRenderer> part : parts.entrySet()) {
            List<String> expected = new ArrayList<>(), actual = new ArrayList<>();
            double[] origin = nativeOrigin(part.getValue(), binding, part.getKey());
            for (Object box : part.getValue().cubeList)
                for (TexturedQuad quad : (TexturedQuad[]) quadList.get(box)) expected.add(quad(quad, origin));
            CemModel.Node slot = loaded.originalParts.get(part.getKey());
            for (CemModel.Node custom : slot.children)
                if (custom.vanillaPart == null) collect(custom, offset(loaded, slot), actual);
            Collections.sort(expected);
            Collections.sort(actual);
            assertEquals(part.getKey(), expected, actual);
        }
    }

    /** A part's pivot in model space, through its native parents. */
    private static double[] nativeOrigin(ModelRenderer part, CemBinding binding, String name) {
        double[] origin = { part.rotationPointX, part.rotationPointY, part.rotationPointZ };
        for (String parent = binding.parents.get(name); parent != null; parent = binding.parents.get(parent)) {
            ModelRenderer above = binding.parts.get(parent);
            origin[0] += above.rotationPointX;
            origin[1] += above.rotationPointY;
            origin[2] += above.rotationPointZ;
        }
        return origin;
    }

    private static double[] offset(CemModel model, CemModel.Node target) {
        double[] sum = new double[3];
        return path(model.root, target, sum) ? sum : null;
    }

    private static boolean path(CemModel.Node node, CemModel.Node target, double[] sum) {
        for (int axis = 0; axis < 3; axis++) sum[axis] += node.transform[axis];
        if (node == target) return true;
        for (CemModel.Node child : node.children) if (path(child, target, sum)) return true;
        for (int axis = 0; axis < 3; axis++) sum[axis] -= node.transform[axis];
        return false;
    }

    private static void collect(CemModel.Node node, double[] origin, List<String> faces) {
        double[] here = origin.clone();
        for (int axis = 0; axis < 3; axis++) here[axis] += node.transform[axis];
        for (CemModel.Box box : node.boxes) for (float[] face : new CemBoxMesh(box).faces) {
            if (face == null) continue;
            String[] vertices = new String[4];
            for (int v = 0; v < 4; v++) vertices[v] = vertex(
                here[0] + face[v * 5],
                here[1] + face[v * 5 + 1],
                here[2] + face[v * 5 + 2],
                face[v * 5 + 3],
                face[v * 5 + 4]);
            faces.add(cycle(vertices));
        }
        for (CemModel.Node child : node.children) collect(child, here, faces);
    }

    private static String quad(TexturedQuad quad, double[] origin) {
        String[] vertices = new String[4];
        for (int v = 0; v < 4; v++) {
            PositionTextureVertex vertex = quad.vertexPositions[v];
            vertices[v] = vertex(
                origin[0] + vertex.vector3D.xCoord,
                origin[1] + vertex.vector3D.yCoord,
                origin[2] + vertex.vector3D.zCoord,
                vertex.texturePositionX,
                vertex.texturePositionY);
        }
        return cycle(vertices);
    }

    private static String vertex(double x, double y, double z, double u, double v) {
        return String.format("(%.3f %.3f %.3f | %.4f %.4f)", x, y, z, u, v);
    }

    /** The same face in the same winding, from whichever corner it starts. */
    private static String cycle(String[] vertices) {
        int start = 0;
        for (int i = 1; i < 4; i++) if (vertices[i].compareTo(vertices[start]) < 0) start = i;
        StringBuilder face = new StringBuilder();
        for (int i = 0; i < 4; i++) face.append(vertices[(start + i) % 4]);
        return face.toString()
            .replace("-0.000", "0.000");
    }
}
