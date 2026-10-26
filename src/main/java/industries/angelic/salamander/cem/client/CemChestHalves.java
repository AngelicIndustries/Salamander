package industries.angelic.salamander.cem.client;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelLargeChest;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/** Splits the old combined large-chest model at its seam for the two standard CEM targets. */
public final class CemChestHalves {

    private static final Half LEFT = new Half(true), RIGHT = new Half(false);
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    private CemChestHalves() {}

    public static void clear() {
        for (ResourceLocation texture : TEXTURES.values()) if (texture.getResourcePath()
            .startsWith("dynamic/"))
            Minecraft.getMinecraft()
                .getTextureManager()
                .deleteTexture(texture);
        TEXTURES.clear();
    }

    public static boolean render(ModelChest chest) {
        if (!(chest instanceof ModelLargeChest) || !industries.angelic.salamander.config.CemConfig.enabled
            || !CemResources.INSTANCE.hasModels()) return false;
        String target = CemTargets.target(CemRuntime.subject());
        if (target == null || !CemResources.INSTANCE.hasTargets(Arrays.asList(target + "_left", target + "_right")))
            return false;
        ResourceLocation texture = CemRuntime.texture();
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0, 1, 1);
            GL11.glScalef(1, -1, -1);
            RIGHT.render(target + "_right", chest.chestLid.rotateAngleX, halfTexture(texture, false));
            GL11.glTranslatef(1, 0, 0);
            LEFT.render(target + "_left", chest.chestLid.rotateAngleX, halfTexture(texture, true));
        } finally {
            GL11.glPopMatrix();
            if (texture != null) Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(texture);
        }
        return true;
    }

    private static final class Half extends ModelBase implements CemModelParts {

        final Map<String, ModelRenderer> parts = new LinkedHashMap<>();

        Half(boolean left) {
            textureWidth = 64;
            textureHeight = 64;
            int x = left ? 0 : 1;
            parts.put("base", new ModelRenderer(this, 0, 19).addBox(x, 0, 1, 15, 10, 14));
            ModelRenderer lid = new ModelRenderer(this, 0, 0).addBox(x, 0, 0, 15, 5, 14);
            lid.setRotationPoint(0, 9, 1);
            parts.put("lid", lid);
            ModelRenderer knob = new ModelRenderer(this, 0, 0).addBox(left ? 0 : 15, -2, 14, 1, 4, 1);
            knob.setRotationPoint(0, 9, 1);
            parts.put("knob", knob);
        }

        @Override
        public Map<String, ModelRenderer> salamander$cemParts() {
            return parts;
        }

        void render(String target, float angle, ResourceLocation texture) {
            parts.get("lid").rotateAngleX = angle;
            parts.get("knob").rotateAngleX = angle;
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(texture);
            CemRuntime.Draw previous = CemRuntime
                .beginNamed(this, null, 0, 0, 0, 0, 0, Collections.singletonList(target));
            try {
                for (ModelRenderer part : parts.values()) part.render(.0625f);
            } finally {
                CemRuntime.end(previous, .0625f);
            }
        }
    }

    private static ResourceLocation halfTexture(ResourceLocation source, boolean left) {
        String path = source.getResourcePath()
            .replace("_double.png", left ? "_left.png" : "_right.png");
        ResourceLocation modern = new ResourceLocation(source.getResourceDomain(), path);
        if (!modern.equals(source) && CemResources.INSTANCE.exists(modern)) return modern;
        return TEXTURES.computeIfAbsent(source + ":" + left, ignored -> {
            try (InputStream stream = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(source)
                .getInputStream()) {
                BufferedImage old = ImageIO.read(stream);
                int scale = Math.max(1, old.getWidth() / 128);
                BufferedImage result = new BufferedImage(64 * scale, 64 * scale, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = result.createGraphics();
                g.scale(scale, scale);
                split(g, old, 0, 0, 30, 14, 5, scale, left);
                split(g, old, 0, 19, 30, 14, 10, scale, left);
                split(g, old, 0, 0, 2, 1, 4, scale, left);
                g.dispose();
                return Minecraft.getMinecraft()
                    .getTextureManager()
                    .getDynamicTextureLocation("salamander_chest", new DynamicTexture(result));
            } catch (Exception exception) {
                return source;
            }
        });
    }

    private static void split(Graphics2D g, BufferedImage image, int u, int v, int width, int depth, int height,
        int scale, boolean left) {
        int half = width / 2, cut = left ? half : 0;
        // Current chest coordinates invert Y and Z. Swap top/bottom and front/back, then rotate their UVs.
        copy(g, image, u + depth, v, u + depth + width + cut, v, half, depth, scale, false, true);
        copy(g, image, u + depth + half, v, u + depth + cut, v, half, depth, scale, false, true);
        copy(g, image, u, v + depth, u, v + depth, depth, height, scale, true, true);
        copy(g, image, u + depth + half, v + depth, u + depth + width, v + depth, depth, height, scale, true, true);
        copy(
            g,
            image,
            u + depth,
            v + depth,
            u + 2 * depth + width + (left ? 0 : half),
            v + depth,
            half,
            height,
            scale,
            true,
            true);
        copy(g, image, u + 2 * depth + half, v + depth, u + depth + cut, v + depth, half, height, scale, true, true);
    }

    private static void copy(Graphics2D g, BufferedImage image, int x, int y, int sx, int sy, int w, int h, int scale,
        boolean flipX, boolean flipY) {
        g.drawImage(
            image,
            x,
            y,
            x + w,
            y + h,
            (sx + (flipX ? w : 0)) * scale,
            (sy + (flipY ? h : 0)) * scale,
            (sx + (flipX ? 0 : w)) * scale,
            (sy + (flipY ? 0 : h)) * scale,
            null);
    }
}
