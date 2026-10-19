package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Collections;

import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.loading.CemPackMetadata;
import industries.angelic.salamander.cem.model.CemBoxMesh;
import industries.angelic.salamander.cem.model.CemModel;

public class CemMetadataAndMeshTest {

    @Test
    public void individualFaceDirectionsAndWingUvOrientationFollowCemCoordinates() throws Exception {
        String json = "{\"models\":[{\"part\":\"body\",\"id\":\"cube\",\"boxes\":["
            + "{\"coordinates\":[0,0,0,9,0,6],\"uvUp\":[6,24,15,18]},"
            + "{\"coordinates\":[0,0,0,9,3,6],\"uvWest\":[1,2,3,4],\"uvEast\":[5,6,7,8],\"uvDown\":[9,10,11,12]}]}]}";
        CemModel model = new CemLoader(location -> new StringReader(json))
            .load(new ResourceLocation("test:wing.jem"), Collections.singletonMap("body", new float[] { 0, 0, 0 }));
        CemModel.Node node = model.find("cube", null, null);
        CemBoxMesh wing = new CemBoxMesh(node.boxes.get(0));
        assertNull(wing.faces[3]);
        float[] up = wing.faces[2];
        assertEquals(-1, up[21], 0);
        // The wing root at x=0,z=0 samples the far U edge, not the transparent wing tip.
        assertEquals(0, up[0], 0);
        assertEquals(0, up[2], 0);
        assertEquals(15 / 64f, up[3], 0);
        assertEquals(24 / 32f, up[4], 0);
        assertEquals(9, up[5], 0);
        assertEquals(6 / 64f, up[8], 0);
        CemBoxMesh cube = new CemBoxMesh(node.boxes.get(1));
        assertEquals(1, cube.faces[0][20], 0);
        assertEquals(3 / 64f, cube.faces[0][3], 0);
        assertEquals(-1, cube.faces[1][20], 0);
        assertEquals(7 / 64f, cube.faces[1][3], 0);
        assertEquals(1, cube.faces[3][21], 0);
        assertEquals(11 / 64f, cube.faces[3][3], 0);
    }

    @Test
    public void acceptsModernMetadataWithoutMutatingItOrChangingLegacyFormat() {
        JsonObject modern = new JsonParser().parse("{\"description\":\"test\",\"min_format\":84,\"max_format\":999}")
            .getAsJsonObject();
        assertEquals(
            1,
            CemPackMetadata.legacyCompatible(modern)
                .getAsJsonObject()
                .get("pack_format")
                .getAsInt());
        assertFalse(modern.has("pack_format"));
        modern.addProperty("pack_format", 15);
        assertSame(modern, CemPackMetadata.legacyCompatible(modern));
        assertEquals(
            15,
            modern.get("pack_format")
                .getAsInt());
    }

    @Test
    public void mirroredAndFractionalBoxesHaveOutwardNormalsAndMatchingWinding() {
        for (boolean mirrorU : new boolean[] { false, true }) for (boolean mirrorV : new boolean[] { false, true }) {
            CemBoxMesh mesh = new CemBoxMesh(
                new CemModel.Box(
                    new float[] { 1, 2, 3, 4.5f, 5, 6 },
                    new float[] { .1f, .2f, .3f },
                    new float[] { 0, 0 },
                    64,
                    64,
                    mirrorU,
                    mirrorV));
            for (float[] face : mesh.faces) {
                double ax = face[5] - face[0], ay = face[6] - face[1], az = face[7] - face[2];
                double bx = face[10] - face[5], by = face[11] - face[6], bz = face[12] - face[7];
                double dot = (ay * bz - az * by) * face[20] + (az * bx - ax * bz) * face[21]
                    + (ax * by - ay * bx) * face[22];
                assertTrue("winding disagrees with face normal", dot > 0);
                for (int i = 0; i < 20; i += 5) {
                    assertTrue(face[i + 3] >= 0 && face[i + 3] <= 1);
                    assertTrue(face[i + 4] >= 0 && face[i + 4] <= 1);
                }
            }
        }
    }
}
