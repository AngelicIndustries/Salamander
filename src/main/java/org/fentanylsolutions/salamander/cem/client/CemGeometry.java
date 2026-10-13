package org.fentanylsolutions.salamander.cem.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.Tessellator;

import org.fentanylsolutions.salamander.cem.model.CemBoxMesh;
import org.fentanylsolutions.salamander.cem.model.CemModel;
import org.fentanylsolutions.salamander.cem.model.CemSpriteMesh;
import org.fentanylsolutions.salamander.compat.wawelauth.WawelAuthCompat;
import org.fentanylsolutions.salamander.compat.wawelauth.WawelAuthOverlays;
import org.lwjgl.opengl.GL11;

/** Baked box vertices rendered through the vanilla Tessellator, with no Angelica dependency or GPU cache. */
public final class CemGeometry {

    private final CemModel model;
    private final List<List<float[]>> faces = new ArrayList<>();
    /** Boxes that are vanilla skin overlay pieces, which Wawel Auth may draw in their place. */
    private final List<List<OverlayBox>> overlays = new ArrayList<>();

    public CemGeometry(CemModel model) {
        this.model = model;
        for (CemModel.Node node : model.nodes) {
            List<float[]> part = new ArrayList<>();
            List<OverlayBox> pieces = new ArrayList<>();
            for (CemModel.Box box : node.boxes) {
                WawelAuthOverlays.Overlay overlay = WawelAuthOverlays.match(box);
                List<float[]> target = part;
                if (overlay != null) {
                    OverlayBox piece = new OverlayBox(overlay);
                    pieces.add(piece);
                    target = piece.faces;
                }
                for (float[] face : new CemBoxMesh(box).faces) if (face != null) target.add(face);
            }
            for (CemModel.Box sprite : node.sprites) part.addAll(new CemSpriteMesh(sprite).faces);
            faces.add(part);
            overlays.add(pieces);
        }
    }

    public void renderNativePart(CemModel.Node node, double[] pose, Map<String, ModelRenderer> vanilla, float scale,
        net.minecraft.util.ResourceLocation texture, boolean rotationOrder, CemRenderFrame frame, boolean atOrigin) {
        boolean mirrored = dragonMirror(node, vanilla);
        GL11.glPushMatrix();
        if (mirrored) {
            GL11.glPushAttrib(GL11.GL_POLYGON_BIT);
            GL11.glScalef(-1, 1, 1);
            GL11.glCullFace(GL11.GL_BACK);
        }
        try {
            if (atOrigin) frame.atOrigin(model.root, pose, scale);
            else frame.insertRoot(model.root, pose, scale);
            renderTextured(node, pose, vanilla, scale, texture, rotationOrder);
        } finally {
            if (mirrored) GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    public void renderRootExtras(double[] pose, Map<String, ModelRenderer> vanilla, float scale,
        net.minecraft.util.ResourceLocation texture, CemRenderFrame frame) {
        if (pose[model.root.index * CemModel.STRIDE + 9] == 0) return;
        GL11.glPushMatrix();
        try {
            frame.atOrigin(model.root, pose, scale);
            for (CemModel.Node node : model.root.children)
                if (node.vanillaPart == null || !vanilla.containsKey(node.vanillaPart))
                    renderTextured(node, pose, vanilla, scale, texture, false);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private void renderTextured(CemModel.Node node, double[] pose, Map<String, ModelRenderer> vanilla, float scale,
        net.minecraft.util.ResourceLocation texture, boolean rotationOrder) {
        net.minecraft.util.ResourceLocation selected = model.texture == null || CemEmissive.eyes(texture)
            || texture != null && texture.equals(CemResources.INSTANCE.emissive(model.texture)) ? texture
                : model.texture;
        boolean eyes = CemEmissive.eyes(texture) || CemResources.INSTANCE.isEmissive(texture);
        if (eyes && model.texture != null) {
            net.minecraft.util.ResourceLocation mask = CemResources.INSTANCE.emissive(model.texture);
            if (mask != null) selected = mask;
        }
        selected = CemResources.INSTANCE.texture(CemRandomTextures.resolve(selected));
        net.minecraft.util.ResourceLocation previousResource = CemRuntime.texture();
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean normalized = GL11.glIsEnabled(GL11.GL_NORMALIZE);
        GL11.glEnable(GL11.GL_NORMALIZE);
        try {
            if (selected != null) net.minecraft.client.Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(selected);
            renderPart(node, pose, vanilla, scale, rotationOrder, selected, eyes, model.texture == null && !eyes);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
            CemRuntime.texture(previousResource);
            if (!normalized) GL11.glDisable(GL11.GL_NORMALIZE);
        }
    }

    public static void transform(CemModel.Node node, double[] pose, float scale, boolean rotationOrder) {
        int offset = node.index * CemModel.STRIDE;
        GL11.glTranslated(pose[offset] * scale, pose[offset + 1] * scale, pose[offset + 2] * scale);
        if (rotationOrder) {
            GL11.glRotated(Math.toDegrees(pose[offset + 4]), 0, 1, 0);
            GL11.glRotated(Math.toDegrees(pose[offset + 3]), 1, 0, 0);
            GL11.glRotated(Math.toDegrees(pose[offset + 5]), 0, 0, 1);
        } else {
            GL11.glRotated(Math.toDegrees(pose[offset + 5]), 0, 0, 1);
            GL11.glRotated(Math.toDegrees(pose[offset + 4]), 0, 1, 0);
            GL11.glRotated(Math.toDegrees(pose[offset + 3]), 1, 0, 0);
        }
        GL11.glScaled(pose[offset + 6], pose[offset + 7], pose[offset + 8]);
    }

    public boolean attachment(String key, double[] pose, float scale) {
        java.util.List<CemModel.Node> chain = new java.util.ArrayList<>();
        if (!findAttachment(model.root, key, chain)) return false;
        for (CemModel.Node child : chain) {
            if (child != model.root) transform(child, pose, scale, false);
            float[] point = child.attachments.get(key);
            if (point != null) GL11.glTranslatef(point[0] * scale, point[1] * scale, point[2] * scale);
        }
        return true;
    }

    private boolean findAttachment(CemModel.Node node, String key, java.util.List<CemModel.Node> chain) {
        chain.add(node);
        if (node.attachments.containsKey(key)) return true;
        for (CemModel.Node child : node.children) if (findAttachment(child, key, chain)) return true;
        chain.remove(chain.size() - 1);
        return false;
    }

    /** {@code skin}: the parts above drew with the entity's own texture, so a player's skin when it is a player. */
    private void renderPart(CemModel.Node node, double[] pose, Map<String, ModelRenderer> vanilla, float scale,
        boolean rotationOrder, net.minecraft.util.ResourceLocation inherited, boolean eyes, boolean skin) {
        int offset = node.index * CemModel.STRIDE;
        if (pose[offset + 9] == 0) return;
        net.minecraft.util.ResourceLocation previousResource = CemRuntime.texture();
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glPushMatrix();
        try {
            ModelRenderer nativePart = vanilla.get(node.vanillaPart);
            if (nativePart != null) GL11.glTranslatef(nativePart.offsetX, nativePart.offsetY, nativePart.offsetZ);
            transform(node, pose, scale, rotationOrder);
            net.minecraft.util.ResourceLocation selected = node.texture == null ? inherited
                : eyes ? CemResources.INSTANCE.emissive(node.texture) : node.texture;
            if (selected != null) net.minecraft.client.Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(selected);
            boolean ownSkin = skin && node.texture == null;
            if (pose[offset + 10] != 0 && (!eyes || selected != null)) {
                if (!eyes && node.vanillaGeometry
                    && CemBoatChest.part(node.vanillaPart)
                    && !vanilla.containsKey(node.vanillaPart)) CemBoatChest.render(node.vanillaPart, scale);
                drawGeometry(node, vanilla, scale, selected, ownSkin);
                if (!eyes) CemEmissive.render(selected, () -> drawGeometry(node, vanilla, scale, selected, false));
            }
            for (CemModel.Node child : node.children)
                renderPart(child, pose, vanilla, scale, false, selected, eyes, ownSkin);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            CemRuntime.texture(previousResource);
            GL11.glPopMatrix();
        }
    }

    private static boolean dragonMirror(CemModel.Node node, Map<String, ModelRenderer> vanilla) {
        return CemBinding.rightDragonPart(node.vanillaPart) && vanilla.containsKey("neck5")
            && vanilla.get("left_wing") == vanilla.get("right_wing");
    }

    /** {@code skin}: the player's own skin is bound, so Wawel Auth may draw the overlay pieces. */
    private void drawGeometry(CemModel.Node node, Map<String, ModelRenderer> vanilla, float scale,
        net.minecraft.util.ResourceLocation texture, boolean skin) {
        ModelRenderer original = node.vanillaGeometry ? vanilla.get(node.vanillaPart) : null;
        if (original != null) {
            int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            net.minecraft.util.ResourceLocation previousResource = CemRuntime.texture();
            net.minecraft.util.ResourceLocation nativeTexture = CemPlayerSkins.nativeTexture(texture, original);
            if (nativeTexture != null && !nativeTexture.equals(texture)) net.minecraft.client.Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(nativeTexture);
            boolean mirrored = dragonMirror(node, vanilla);
            if (mirrored) {
                GL11.glPushMatrix();
                GL11.glPushAttrib(GL11.GL_POLYGON_BIT);
                GL11.glScalef(-1, 1, 1);
                GL11.glCullFace(GL11.GL_FRONT);
            }
            try {
                for (ModelBox box : original.cubeList) box.render(Tessellator.instance, scale);
            } finally {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
                CemRuntime.texture(previousResource);
                if (mirrored) {
                    GL11.glPopAttrib();
                    GL11.glPopMatrix();
                }
            }
        }
        draw(faces.get(node.index), scale);
        for (OverlayBox piece : overlays.get(node.index))
            if (!skin || !WawelAuthCompat.submit(piece.overlay)) draw(piece.faces, scale);
    }

    private static void draw(List<float[]> geometry, float scale) {
        if (geometry.isEmpty()) return;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (float[] face : geometry) {
            tessellator.setNormal(face[20], face[21], face[22]);
            for (int vertex = 0; vertex < 20; vertex += 5) tessellator.addVertexWithUV(
                face[vertex] * scale,
                face[vertex + 1] * scale,
                face[vertex + 2] * scale,
                face[vertex + 3],
                face[vertex + 4]);
        }
        tessellator.draw();
    }

    private static final class OverlayBox {

        final WawelAuthOverlays.Overlay overlay;
        final List<float[]> faces = new ArrayList<>();

        OverlayBox(WawelAuthOverlays.Overlay overlay) {
            this.overlay = overlay;
        }
    }
}
