package industries.angelic.salamander.cem.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

public class CemArmorSlicesTest {

    private static final float EPSILON = 1.0e-4f;
    // The split sleeve from the test pack: upper half on the arm, lower half on an elbow child six rows down.
    private static final String SPLIT_SLEEVE = "{\"part\":\"right_sleeve\",\"id\":\"right_sleeve\",\"boxes\":[{"
        + "\"coordinates\":[-3,-2,-2,4,6,4],\"textureOffset\":[40,32],\"sizeAdd\":0.25}],\"submodels\":[{\"id\":\"elbow\","
        + "\"translate\":[-1,4,0],\"boxes\":[{\"coordinates\":[-2,0,-2,4,6,4],\"textureOffset\":[40,38],"
        + "\"sizeAdd\":0.25}]}]}";

    @Test
    public void armorPiecesGoWhereThePackPutsTheSleeveSlices() throws Exception {
        CemModel model = model(SPLIT_SLEEVE);
        List<CemGeometry.Slice> slices = new CemGeometry(model).slices("RIGHT_SLEEVE");
        assertEquals(2, slices.size());
        assertEquals("elbow", slices.get(1).path.get(slices.get(1).path.size() - 1).id);
        // Upper: the vanilla piece sits where vanilla has it. Lower: the arm's pivot is above and beside the elbow.
        assertOffset(slices.get(0), 0, 0, 0);
        assertOffset(slices.get(1), 1, -4, 0);
        // The elbow, between the two halves' rows, six rows below the arm's top at y -2.
        assertEquals(4, CemArmorSlices.joint(slices.get(0).overlay, slices.get(1).overlay), EPSILON);
        assertTrue(
            new CemGeometry(model).slices("LEFT_SLEEVE")
                .isEmpty());
    }

    @Test
    public void chestplateArmSplitsIntoClosedHalves() throws Exception {
        // Vanilla chestplate arms: 4x12x4 from y -2, inflated by 1, on a 64x32 sheet, sides on rows 20..32.
        ModelBiped chestplate = new ModelBiped(1.0F);
        for (ModelRenderer arm : new ModelRenderer[] { chestplate.bipedRightArm, chestplate.bipedLeftArm }) {
            List<TexturedQuad> faces = faces(arm);
            List<TexturedQuad> upper = CemArmorSlices.split(faces, -2, 10, Float.NEGATIVE_INFINITY, 4);
            List<TexturedQuad> lower = CemArmorSlices.split(faces, -2, 10, 4, Float.POSITIVE_INFINITY);
            // Both halves reach past the elbow by the inflation. The upper half is closed below by the row the cut
            // runs along; the lower half's top stays open inside it.
            assertEquals(6, upper.size());
            assertEquals(5, lower.size());
            assertSpan(upper, -3, 5, 20 / 32f, 26 / 32f);
            assertSpan(lower, 3, 11, 26 / 32f, 32 / 32f);
            assertCapRow(upper, 5, 25.5f / 32);
            for (TexturedQuad quad : lower) assertTrue(!flatAt(quad, 3));
            // Mirroring survives: each face keeps its source face's corners in order, so its winding too.
            for (TexturedQuad quad : upper) assertTrue(fromFace(faces, quad));
            for (TexturedQuad quad : lower) assertTrue(fromFace(faces, quad));
        }
    }

    @Test
    public void bootsGoWithTheShin() throws Exception {
        // Boots paint the leg's lower six rows, 26..32; split at the knee they all ride the lower half.
        List<TexturedQuad> faces = faces(new ModelBiped(1.0F).bipedRightLeg);
        assertSpan(CemArmorSlices.split(faces, 0, 12, 6, Float.POSITIVE_INFINITY), 5, 13, 26 / 32f, 32 / 32f);
        List<TexturedQuad> thigh = CemArmorSlices.split(faces, 0, 12, Float.NEGATIVE_INFINITY, 6);
        assertSpan(thigh, -1, 7, 20 / 32f, 26 / 32f);
        // The thigh half's knee cap shows row 5, which boots leave empty, not the sole.
        assertCapRow(thigh, 7, 25.5f / 32);
    }

    /** The share's flat face at height {@code y} samples just texture row {@code v}. */
    private static void assertCapRow(List<TexturedQuad> share, float y, float v) {
        boolean found = false;
        for (TexturedQuad quad : share) if (flatAt(quad, y)) {
            found = true;
            for (PositionTextureVertex vertex : quad.vertexPositions) assertEquals(v, vertex.texturePositionY, EPSILON);
        }
        assertTrue(found);
    }

    private static boolean flatAt(TexturedQuad quad, float y) {
        for (PositionTextureVertex vertex : quad.vertexPositions)
            if (Math.abs(vertex.vector3D.yCoord - y) > EPSILON) return false;
        return true;
    }

    @Test
    public void boxesOnOneSideStayWhole() throws Exception {
        // A shoulder pad above the elbow rides the upper arm as it is, and leaves the forearm nothing.
        ModelRenderer pad = new ModelRenderer(new ModelBase() {}, 40, 16);
        pad.setTextureSize(64, 32);
        pad.addBox(-3, -3, -2, 4, 4, 4, 0.5F);
        List<TexturedQuad> faces = faces(pad);
        assertSame(faces, CemArmorSlices.split(faces, -3, 1, Float.NEGATIVE_INFINITY, 4));
        assertTrue(
            CemArmorSlices.split(faces, -3, 1, 4, Float.POSITIVE_INFINITY)
                .isEmpty());
    }

    /** The share's height from its top cap to its bottom cap, and the texture rows its sides show. */
    private static void assertSpan(List<TexturedQuad> share, float top, float bottom, float firstRow, float lastRow) {
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
        for (TexturedQuad quad : share) {
            boolean side = false;
            for (PositionTextureVertex vertex : quad.vertexPositions) {
                minY = Math.min(minY, vertex.vector3D.yCoord);
                maxY = Math.max(maxY, vertex.vector3D.yCoord);
                side |= Math.abs(vertex.vector3D.yCoord - quad.vertexPositions[0].vector3D.yCoord) > EPSILON;
            }
            if (side) for (PositionTextureVertex vertex : quad.vertexPositions) {
                minV = Math.min(minV, vertex.texturePositionY);
                maxV = Math.max(maxV, vertex.texturePositionY);
            }
        }
        assertEquals(top, minY, EPSILON);
        assertEquals(bottom, maxY, EPSILON);
        assertEquals(firstRow, minV, EPSILON);
        assertEquals(lastRow, maxV, EPSILON);
    }

    /** Whether a source face has the cut face's corners in the same order, apart from their heights. */
    private static boolean fromFace(List<TexturedQuad> faces, TexturedQuad cut) {
        for (TexturedQuad face : faces) {
            boolean same = true;
            for (int i = 0; i < 4 && same; i++) {
                net.minecraft.util.Vec3 a = face.vertexPositions[i].vector3D, b = cut.vertexPositions[i].vector3D;
                same = Math.abs(a.xCoord - b.xCoord) < EPSILON && Math.abs(a.zCoord - b.zCoord) < EPSILON;
            }
            if (same) return true;
        }
        return false;
    }

    private static void assertOffset(CemGeometry.Slice slice, float x, float y, float z) {
        float[] offset = CemArmorSlices.offset(slice.overlay);
        assertEquals(x, offset[0], EPSILON);
        assertEquals(y, offset[1], EPSILON);
        assertEquals(z, offset[2], EPSILON);
    }

    private static List<TexturedQuad> faces(ModelRenderer part) throws ReflectiveOperationException {
        Field quads = ModelBox.class.getDeclaredField("quadList");
        quads.setAccessible(true);
        List<TexturedQuad> faces = new ArrayList<>();
        for (Object box : part.cubeList) Collections.addAll(faces, (TexturedQuad[]) quads.get(box));
        return faces;
    }

    private static CemModel model(String entry) throws Exception {
        ResourceLocation source = new ResourceLocation("minecraft", "optifine/cem/player.jem");
        Map<String, float[]> pivots = new LinkedHashMap<>();
        pivots.put("right_sleeve", new float[3]);
        pivots.put("left_sleeve", new float[3]);
        String document = "{\"textureSize\":[64,64],\"models\":[" + entry + "]}";
        return new CemLoader(location -> {
            if (!location.equals(source)) throw new FileNotFoundException(location.toString());
            return new StringReader(document);
        }).load(source, pivots, Collections.emptyMap(), 64, 64);
    }
}
