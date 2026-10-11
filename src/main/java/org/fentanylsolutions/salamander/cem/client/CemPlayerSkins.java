package org.fentanylsolutions.salamander.cem.client;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

import org.fentanylsolutions.salamander.compat.wawelauth.WawelAuthCompat;

/** Player-pack atlases are separate textures so reloads and native rendering keep their original skin. */
public final class CemPlayerSkins {

    private static final Map<ResourceLocation, Skin> SKINS = new LinkedHashMap<>(16, .75f, true);
    private static final Map<ResourceLocation, ResourceLocation> NATIVE = new HashMap<>();

    private CemPlayerSkins() {}

    /** A player's own Wawel Auth skin supplies its pixels, unless the pack replaces the texture. */
    public static ResourceLocation texture(ResourceLocation original, Object subject) {
        if (original == null) return null;
        Minecraft minecraft = Minecraft.getMinecraft();
        ResourceLocation selected = CemResources.INSTANCE.texture(original);
        ITextureObject source = minecraft.getTextureManager()
            .getTexture(selected);
        WawelAuthCompat.SkinPixels wawel = selected.equals(original) ? WawelAuthCompat.skin(subject, original) : null;
        BufferedImage image = wawel == null && source instanceof CemSkinTexture
            ? ((CemSkinTexture) source).salamander$skinImage()
            : null;
        Object version = wawel != null ? (Object) wawel.revision : image;
        Skin cached = SKINS.get(original);
        if (cached != null && cached.source == source && Objects.equals(cached.version, version)) return cached.texture;
        BufferedImage pixels = wawel != null ? wawel.image() : image;
        if (pixels == null) {
            try (InputStream stream = minecraft.getResourceManager()
                .getResource(selected)
                .getInputStream()) {
                pixels = ImageIO.read(stream);
            } catch (java.io.IOException ignored) {
                return original;
            }
        }
        BufferedImage normalized = CemSkinImage.normalize(pixels);
        if (normalized == null) return original;
        if (cached != null) delete(cached);
        int previousTexture = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);
        ResourceLocation previousResource = CemRuntime.texture();
        ResourceLocation texture;
        try {
            texture = minecraft.getTextureManager()
                .getDynamicTextureLocation("salamander_player_skin", new DynamicTexture(normalized));
        } finally {
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, previousTexture);
            CemRuntime.texture(previousResource);
        }
        SKINS.put(original, new Skin(source, version, texture));
        NATIVE.put(texture, original);
        if (SKINS.size() > 256) {
            java.util.Iterator<Skin> iterator = SKINS.values()
                .iterator();
            delete(iterator.next());
            iterator.remove();
        }
        return texture;
    }

    public static ResourceLocation nativeTexture(ResourceLocation texture, ModelRenderer part) {
        return part.textureWidth == part.textureHeight * 2 ? NATIVE.getOrDefault(texture, texture) : texture;
    }

    public static void clear() {
        for (Skin skin : SKINS.values()) delete(skin);
        SKINS.clear();
    }

    private static void delete(Skin skin) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .deleteTexture(skin.texture);
        NATIVE.remove(skin.texture);
    }

    private static final class Skin {

        final ITextureObject source;
        /** The captured image, compared by identity, or Wawel Auth's skin revision. */
        final Object version;
        final ResourceLocation texture;

        Skin(ITextureObject source, Object version, ResourceLocation texture) {
            this.source = source;
            this.version = version;
            this.texture = texture;
        }
    }
}
