package industries.angelic.salamander.cem.client;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

public class CemOverlaysTest {

    private static final ResourceLocation SOURCE = new ResourceLocation("minecraft", "optifine/cem/player.jem");

    // Boxes as Fresh Moves v3.1.1 ships them: pre-inflated sizes, per-face UVs, inverted X and Y.
    private static final String JACKET = "{\"part\":\"jacket\",\"id\":\"jacket\",\"invertAxis\":\"xy\","
        + "\"translate\":[0,-24,0],\"boxes\":[{\"coordinates\":[-4.25,11.75,-2.25,8.5,12.5,4.5],"
        + "\"uvNorth\":[20,36,28,48],\"uvEast\":[16,36,20,48],\"uvSouth\":[32,36,40,48],\"uvWest\":[28,36,32,48],"
        + "\"uvUp\":[28,36,20,32],\"uvDown\":[36,32,28,36]}]}";
    private static final String SLIM_SLEEVE = "{\"part\":\"right_sleeve\",\"id\":\"right_sleeve\",\"invertAxis\":\"xy\","
        + "\"translate\":[-5,-22,0],\"boxes\":[{\"coordinates\":[3.75,11.75,-2.25,3.5,12.5,4.5],"
        + "\"uvNorth\":[44,36,47,48],\"uvEast\":[40,36,44,48],\"uvSouth\":[51,36,54,48],\"uvWest\":[47,36,51,48],"
        + "\"uvUp\":[47,36,44,32],\"uvDown\":[50,32,47,36]}]}";
    private static final String HAT = "{\"part\":\"headwear\",\"id\":\"headwear\",\"invertAxis\":\"xy\","
        + "\"translate\":[0,-24,0],\"boxes\":[{\"coordinates\":[-4.5,23.5,-4.5,9,9,9],"
        + "\"uvNorth\":[40,8,48,16],\"uvEast\":[32,8,40,16],\"uvSouth\":[56,8,64,16],\"uvWest\":[48,8,56,16],"
        + "\"uvUp\":[48,8,40,0],\"uvDown\":[56,0,48,8]}]}";

    @Test
    public void freshMovesOverlaysAreVanillaPieces() throws Exception {
        assertOverlay(only(JACKET), "JACKET", -4, -24, -2, 8, 12, 4, 16, 32);
        assertOverlay(only(SLIM_SLEEVE), "RIGHT_SLEEVE", -7, -24, -2, 3, 12, 4, 40, 32);
        assertOverlay(only(HAT), "HAT", -4, -32, -4, 8, 8, 8, 32, 0);
    }

    @Test
    public void boxUvPiecesMatchWithSizeAdd() throws Exception {
        String hat = "{\"part\":\"headwear\",\"id\":\"hat\",\"boxes\":[{\"coordinates\":[-4,-8,-4,8,8,8],"
            + "\"textureOffset\":[32,0],\"sizeAdd\":0.5}]}";
        assertOverlay(only(hat), "HAT", -4, -8, -4, 8, 8, 8, 32, 0);
    }

    @Test
    public void slightlyShrunkPiecesStillMatch() throws Exception {
        // Fresh Animations' player hat and jacket, a hair inside the vanilla inflation.
        String hat = "{\"part\":\"headwear\",\"id\":\"hat\",\"boxes\":[{\"coordinates\":[-4,-8,-4,8,8,8],"
            + "\"textureOffset\":[32,0],\"sizeAdd\":0.49}]}";
        String jacket = "{\"part\":\"jacket\",\"id\":\"jacket\",\"boxes\":[{\"coordinates\":[-4,0,-2,8,12,4],"
            + "\"textureOffset\":[16,32],\"sizeAdd\":0.24}]}";
        assertNotNull(only(hat));
        assertNotNull(only(jacket));
    }

    @Test
    public void otherBoxesAreLeftToThePack() throws Exception {
        // Shifted front face, wrong inflation, mirrored, and a plain body box.
        assertNull(only(JACKET.replace("\"uvNorth\":[20,36,28,48]", "\"uvNorth\":[21,36,29,48]")));
        assertNull(only(JACKET.replace("8.5,12.5,4.5", "8.6,12.6,4.6")));
        assertNull(
            only(
                "{\"part\":\"headwear\",\"id\":\"hat\",\"mirrorTexture\":\"u\",\"boxes\":[{\"coordinates\":"
                    + "[-4,-8,-4,8,8,8],\"textureOffset\":[32,0],\"sizeAdd\":0.5}]}"));
        assertNull(
            only(
                "{\"part\":\"jacket\",\"id\":\"body\",\"boxes\":[{\"coordinates\":[-4,0,-2,8,12,4],"
                    + "\"textureOffset\":[16,16]}]}"));
    }

    @Test
    public void boxUvSlicesOfASplitSleeve() throws Exception {
        // Upper sleeve on the arm's pivot, lower sleeve on an elbow child: the lower box's UV moves down six rows.
        List<CemOverlays.Overlay> slices = all(
            "{\"part\":\"right_sleeve\",\"id\":\"right_sleeve\",\"boxes\":[{\"coordinates\":[-3,-2,-2,4,6,4],"
                + "\"textureOffset\":[40,32],\"sizeAdd\":0.25}],\"submodels\":[{\"id\":\"elbow\",\"translate\":[0,4,0],"
                + "\"boxes\":[{\"coordinates\":[-3,0,-2,4,6,4],\"textureOffset\":[40,38],\"sizeAdd\":0.25}]}]}");
        assertEquals(2, slices.size());
        assertOverlay(slices.get(0), "RIGHT_SLEEVE", -3, -2, -2, 4, 12, 4, 40, 32);
        assertRows(slices.get(0), 0, 6);
        // The lower slice's whole box would start six rows above it, in the elbow's frame.
        assertOverlay(slices.get(1), "RIGHT_SLEEVE", -3, -6, -2, 4, 12, 4, 40, 32);
        assertRows(slices.get(1), 6, 12);
    }

    @Test
    public void perFaceSlicesOfASplitSleeve() throws Exception {
        // Fresh Moves' slim sleeve cut at the elbow: pre-inflated halves, sides cut to rows 0..6 and 6..12, the real
        // top and bottom on their ends and whatever at the cut.
        String upper = "{\"coordinates\":[3.75,17.75,-2.25,3.5,6.5,4.5],\"uvNorth\":[44,36,47,42],"
            + "\"uvEast\":[40,36,44,42],\"uvSouth\":[51,36,54,42],\"uvWest\":[47,36,51,42],\"uvUp\":[47,36,44,32],"
            + "\"uvDown\":[44,42,47,43]}";
        String lower = "{\"coordinates\":[3.75,11.75,-2.25,3.5,6.5,4.5],\"uvNorth\":[44,42,47,48],"
            + "\"uvEast\":[40,42,44,48],\"uvSouth\":[51,42,54,48],\"uvWest\":[47,42,51,48],\"uvDown\":[50,32,47,36]}";
        List<CemOverlays.Overlay> slices = all(
            "{\"part\":\"right_sleeve\",\"id\":\"right_sleeve\",\"invertAxis\":\"xy\",\"translate\":[-5,-22,0],"
                + "\"boxes\":["
                + upper
                + ","
                + lower
                + "]}");
        assertEquals(2, slices.size());
        // Both halves describe the same vanilla sleeve as the uncut Fresh Moves box.
        assertOverlay(slices.get(0), "RIGHT_SLEEVE", -7, -24, -2, 3, 12, 4, 40, 32);
        assertRows(slices.get(0), 0, 6);
        assertOverlay(slices.get(1), "RIGHT_SLEEVE", -7, -24, -2, 3, 12, 4, 40, 32);
        assertRows(slices.get(1), 6, 12);
    }

    @Test
    public void overlappingSlicesCoverTheLimb() throws Exception {
        List<CemOverlays.Overlay> slices = all(
            "{\"part\":\"right_pants\",\"id\":\"right_pants\",\"boxes\":[{\"coordinates\":[-2,0,-2,4,7,4],"
                + "\"textureOffset\":[0,32],\"sizeAdd\":0.25},{\"coordinates\":[-2,5,-2,4,7,4],"
                + "\"textureOffset\":[0,37],\"sizeAdd\":0.25}]}");
        assertRows(slices.get(0), 0, 7);
        assertRows(slices.get(1), 5, 12);
    }

    @Test
    public void slicesThatLeaveRowsOutStayWithThePack() throws Exception {
        // A shortened sleeve, and halves with a gap: Wawel Auth would leave an open end.
        List<CemOverlays.Overlay> stubby = all(
            "{\"part\":\"right_sleeve\",\"id\":\"right_sleeve\",\"boxes\":[{\"coordinates\":[-3,-2,-2,4,8,4],"
                + "\"textureOffset\":[40,32],\"sizeAdd\":0.25}]}");
        assertNull(stubby.get(0));
        List<CemOverlays.Overlay> gap = all(
            "{\"part\":\"right_sleeve\",\"id\":\"right_sleeve\",\"boxes\":[{\"coordinates\":[-3,-2,-2,4,5,4],"
                + "\"textureOffset\":[40,32],\"sizeAdd\":0.25},{\"coordinates\":[-3,4,-2,4,6,4],"
                + "\"textureOffset\":[40,38],\"sizeAdd\":0.25}]}");
        assertNull(gap.get(0));
        assertNull(gap.get(1));
    }

    private static void assertRows(CemOverlays.Overlay overlay, int start, int end) {
        assertNotNull(overlay);
        assertEquals(start, overlay.rowStart);
        assertEquals(end, overlay.rowEnd);
    }

    private static void assertOverlay(CemOverlays.Overlay overlay, String part, float x, float y, float z, int width,
        int height, int depth, int u, int v) {
        assertNotNull(overlay);
        assertEquals(part, overlay.part);
        assertEquals(x, overlay.x, 1.0e-4f);
        assertEquals(y, overlay.y, 1.0e-4f);
        assertEquals(z, overlay.z, 1.0e-4f);
        assertEquals(width, overlay.width);
        assertEquals(height, overlay.height);
        assertEquals(depth, overlay.depth);
        assertEquals(u, overlay.u);
        assertEquals(v, overlay.v);
    }

    /** The match for the single box in a player model holding just this entry. */
    private static CemOverlays.Overlay only(String entry) throws Exception {
        List<CemOverlays.Overlay> matches = all(entry);
        assertEquals(1, matches.size());
        return matches.get(0);
    }

    /** The match for each box, in model order, in a player model holding just this entry; null for no match. */
    private static List<CemOverlays.Overlay> all(String entry) throws Exception {
        Map<String, float[]> pivots = new LinkedHashMap<>();
        for (String part : new String[] { "headwear", "jacket", "right_sleeve", "right_pants" })
            pivots.put(part, new float[3]);
        String document = "{\"textureSize\":[64,64],\"models\":[" + entry + "]}";
        CemModel model = new CemLoader(location -> {
            if (!location.equals(SOURCE)) throw new FileNotFoundException(location.toString());
            return new StringReader(document);
        }).load(SOURCE, pivots, java.util.Collections.emptyMap(), 64, 64);
        Map<CemModel.Box, CemOverlays.Overlay> matches = CemOverlays.matchAll(model);
        List<CemOverlays.Overlay> result = new ArrayList<>();
        for (CemModel.Node node : model.nodes) for (CemModel.Box box : node.boxes) result.add(matches.get(box));
        return result;
    }
}
