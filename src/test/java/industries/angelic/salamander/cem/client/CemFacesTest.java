package industries.angelic.salamander.cem.client;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemModel;

public class CemFacesTest {

    /** The template player's parts, as OptiFine's template model and Just Expressions write them. */
    private static final String[] BODY = { "headwear", "0,-24,0", "-4,24,-4,8,8,8", "32,0", "0.5", "body", "0,-24,0",
        "-4,12,-2,8,12,4", "16,16", "0", "jacket", "0,-24,0", "-4,12,-2,8,12,4", "16,32", "0.25", "right_arm",
        "-5,-22,0", "4,12,-2,4,12,4", "40,16", "0", "right_sleeve", "-5,-22,0", "4,12,-2,4,12,4", "40,32", "0.25",
        "left_arm", "5,-22,0", "-8,12,-2,4,12,4", "32,48", "0", "left_sleeve", "5,-22,0", "-8,12,-2,4,12,4", "48,48",
        "0.25", "right_leg", "-2,-12,0", "0,0,-2,4,12,4", "0,16", "0", "right_pants", "-2,-12,0", "0,0,-2,4,12,4",
        "0,32", "0.25", "left_leg", "2,-12,0", "-4,0,-2,4,12,4", "16,48", "0", "left_pants", "2,-12,0",
        "-4,0,-2,4,12,4", "0,48", "0.25" };

    private static String part(String name, String translate, String box, String uv, String add) {
        return "{\"part\":\"" + name
            + "\",\"id\":\""
            + name
            + "\",\"invertAxis\":\"xy\",\"translate\":["
            + translate
            + "],\"boxes\":[{\"coordinates\":["
            + box
            + "],\"textureOffset\":["
            + uv
            + "],\"sizeAdd\":"
            + add
            + "}]}";
    }

    private static String body(Map<String, String> replacements) {
        StringBuilder parts = new StringBuilder();
        for (int i = 0; i < BODY.length; i += 5) {
            String name = BODY[i];
            String own = replacements.containsKey(name) ? replacements.get(name)
                : part(name, BODY[i + 1], BODY[i + 2], BODY[i + 3], BODY[i + 4]);
            if (!own.isEmpty()) parts.append(',')
                .append(own);
        }
        return parts.toString();
    }

    /** A face as Just Expressions adds it: a second entry on the head, eyes in it, animated. */
    private static final String FACE = "{\"part\":\"head\",\"id\":\"player_face\",\"submodels\":["
        + "{\"id\":\"eyes\",\"invertAxis\":\"xy\",\"translate\":[0,4,-4],\"submodels\":["
        + "{\"id\":\"r_eye\",\"invertAxis\":\"xy\",\"mirrorTexture\":\"u\",\"translate\":[2,-2,1],"
        + "\"boxes\":[{\"coordinates\":[-2,26,-5,2,1,0],\"textureOffset\":[0,0]}]}]}],"
        + "\"animations\":[{\"r_eye.sy\":\"1 - sin(age)\",\"head.ry\":\"head.ry + 0.1\",\"headwear.ry\":\"head.ry\"}]}";
    private static final String HEAD = part("head", "0,-24,0", "-4,24,-4,8,8,8", "0,0", "0");

    private static CemModel load(String models) throws Exception {
        Map<String, float[]> pivots = new LinkedHashMap<>();
        for (String name : new String[] { "head", "headwear", "body", "jacket", "right_arm", "right_sleeve", "left_arm",
            "left_sleeve", "right_leg", "right_pants", "left_leg", "left_pants" })
            pivots.put(name, new float[] { 0, 0, 0 });
        return new CemLoader(location -> new StringReader("{\"textureSize\":[64,64],\"models\":[" + models + "]}"))
            .load(new ResourceLocation("test:optifine/cem/player.jem"), pivots, new HashMap<>(), 64, 64);
    }

    @Test
    public void aFaceOnTheVanillaBodyIsAFace() throws Exception {
        CemModel model = load(FACE + "," + HEAD + body(new HashMap<>()));
        CemFaces.Face face = CemFaces.analyze(model);
        assertEquals(face.reason, CemFaces.Coverage.FACE, face.coverage);
        // Both head entries and the hat draw on the host's head, the head's and hat's cubes left to the host.
        assertEquals(3, face.roots.size());
        java.util.Set<String> skipped = new java.util.HashSet<>();
        for (CemModel.Node node : face.skipped) skipped.add(node.id);
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList("head", "headwear")), skipped);
    }

    @Test
    public void slimArmsAreVanillaToo() throws Exception {
        Map<String, String> slim = new HashMap<>();
        slim.put("right_arm", part("right_arm", "-5,-22,0", "4,12,-2,3,12,4", "40,16", "0"));
        slim.put("left_arm", part("left_arm", "5,-22,0", "-7,12,-2,3,12,4", "32,48", "0"));
        assertEquals(CemFaces.Coverage.FACE, CemFaces.analyze(load(FACE + "," + HEAD + body(slim))).coverage);
    }

    @Test
    public void theVanillaTemplateIsAnEmptyFace() throws Exception {
        CemFaces.Face face = CemFaces.analyze(load(HEAD + body(new HashMap<>())));
        assertEquals(CemFaces.Coverage.FACE, face.coverage);
        assertEquals(face.roots.size(), face.skipped.size());
    }

    @Test
    public void bodyAnimationsCoverTheBody() throws Exception {
        String animated = FACE.replace("{\"r_eye.sy\"", "{\"body.rx\":\"sin(age)\",\"r_eye.sy\"");
        CemFaces.Face face = CemFaces.analyze(load(animated + "," + HEAD + body(new HashMap<>())));
        assertEquals(CemFaces.Coverage.BODY, face.coverage);
        assertTrue(face.reason, face.reason.contains("body.rx"));
    }

    @Test
    public void movingTheHeadCoversTheBody() throws Exception {
        String moved = FACE.replace("\"head.ry\":\"head.ry + 0.1\"", "\"head.ty\":\"2\"");
        assertEquals(
            CemFaces.Coverage.BODY,
            CemFaces.analyze(load(moved + "," + HEAD + body(new HashMap<>()))).coverage);
    }

    @Test
    public void otherBoxesCoverTheBody() throws Exception {
        Map<String, String> longer = new HashMap<>();
        longer.put("right_leg", part("right_leg", "-2,-12,0", "0,0,-2,4,14,4", "0,16", "0"));
        assertEquals(CemFaces.Coverage.BODY, CemFaces.analyze(load(FACE + "," + HEAD + body(longer))).coverage);
        Map<String, String> gone = new HashMap<>();
        gone.put("left_pants", "");
        String removed = FACE + "," + HEAD + body(gone) + ",{\"part\":\"left_pants\",\"id\":\"left_pants\"}";
        assertEquals(CemFaces.Coverage.BODY, CemFaces.analyze(load(removed)).coverage);
        String tail = FACE + ","
            + HEAD
            + body(new HashMap<>())
            + ",{\"part\":\"body\",\"id\":\"tail\",\"attach\":true,\"boxes\":"
            + "[{\"coordinates\":[0,10,2,1,1,6],\"textureOffset\":[0,0]}]}";
        assertEquals(CemFaces.Coverage.BODY, CemFaces.analyze(load(tail)).coverage);
    }

    @Test
    public void aHeadOfAnotherShapeCoversTheBody() throws Exception {
        String big = part("head", "0,-24,0", "-5,24,-5,10,10,10", "0,0", "0");
        assertEquals(CemFaces.Coverage.BODY, CemFaces.analyze(load(FACE + "," + big + body(new HashMap<>()))).coverage);
    }
}
