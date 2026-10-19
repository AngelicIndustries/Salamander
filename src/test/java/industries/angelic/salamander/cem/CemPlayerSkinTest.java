package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Test;

import industries.angelic.salamander.cem.client.CemPlayers;
import industries.angelic.salamander.cem.client.CemSkinImage;
import industries.angelic.salamander.cem.client.CemSkinImages;

public class CemPlayerSkinTest {

    @Test
    public void modernSkinsKeepTransparentFacePixelsAndIndependentLimbs() {
        BufferedImage original = skin(64, 64);
        original.setRGB(10, 12, 0x00112233);
        original.setRGB(36, 52, 0x80445566);
        BufferedImage normalized = CemSkinImage.normalize(original);
        assertNotSame(original, normalized);
        assertEquals(0x00112233, normalized.getRGB(10, 12));
        assertEquals(0x80445566, normalized.getRGB(36, 52));
        normalized.setRGB(0, 0, 0);
        assertEquals(0xff000000, original.getRGB(0, 0));
    }

    @Test
    public void legacySkinsMirrorIndividualLimbFacesAndApplyLegacyAlphaRules() {
        for (int unit : new int[] { 1, 2 }) {
            BufferedImage original = skin(64 * unit, 32 * unit);
            original.setRGB(10 * unit, 12 * unit, 0x00112233);
            BufferedImage normalized = CemSkinImage.normalize(original);
            assertEquals(64 * unit, normalized.getHeight());
            assertEquals(0xff112233, normalized.getRGB(10 * unit, 12 * unit));
            assertEquals(original.getRGB(8 * unit - 1, 20 * unit), normalized.getRGB(20 * unit, 52 * unit));
            assertEquals(original.getRGB(12 * unit - 1, 20 * unit), normalized.getRGB(16 * unit, 52 * unit));
            assertEquals(original.getRGB(48 * unit - 1, 20 * unit), normalized.getRGB(36 * unit, 52 * unit));
            assertEquals(original.getRGB(8 * unit - 1, 16 * unit), normalized.getRGB(20 * unit, 48 * unit));
            assertEquals(0, normalized.getRGB(40 * unit, 8 * unit) >>> 24);
            assertEquals(0, normalized.getRGB(20 * unit, 36 * unit));
            assertEquals(0x00112233, original.getRGB(10 * unit, 12 * unit));
        }
    }

    @Test
    public void rawImageTransferKeepsPixelsLostByNativeProcessing() {
        BufferedImage raw = skin(64, 64);
        BufferedImage processed = skin(64, 32);
        CemSkinImages.remember(processed, raw);
        assertSame(raw, CemSkinImages.original(processed));
        assertSame(raw, CemSkinImages.original(raw));
        assertNull(CemSkinImage.normalize(null));
        assertNull(CemSkinImage.normalize(new BufferedImage(64, 48, BufferedImage.TYPE_INT_ARGB)));
    }

    @Test
    public void profileMetadataSelectsSlimArmsAndInvalidDataFallsBackToWide() {
        assertTrue(CemPlayers.slimProfile(encoded("{\"textures\":{\"SKIN\":{\"metadata\":{\"model\":\"slim\"}}}}")));
        assertFalse(CemPlayers.slimProfile(encoded("{\"textures\":{\"SKIN\":{}}}")));
        assertFalse(CemPlayers.slimProfile(encoded("{}")));
        assertFalse(CemPlayers.slimProfile("invalid base64"));
    }

    private static String encoded(String json) {
        return Base64.getEncoder()
            .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static BufferedImage skin(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) image.setRGB(x, y, 0xff000000 | y << 12 | x);
        return image;
    }
}
