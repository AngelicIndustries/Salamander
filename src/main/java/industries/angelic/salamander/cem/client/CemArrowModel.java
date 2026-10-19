package industries.angelic.salamander.cem.client;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.MathHelper;

import org.lwjgl.opengl.GL11;

import industries.angelic.salamander.cem.model.CemBoxMesh;
import industries.angelic.salamander.cem.model.CemModel;

/** Gives the three quads of the legacy arrow renderer named CEM parts. */
public final class CemArrowModel extends ModelBase implements CemModelParts {

    private final Map<String, ModelRenderer> parts = new LinkedHashMap<>();

    public CemArrowModel() {
        textureWidth = textureHeight = 32;
        add(
            "back",
            new float[] { -7, -2, -2, 0, 4, 4 },
            new float[][] { { 0, 5, 5, 10 }, { 0, 5, 5, 10 }, null, null, null, null });
        add(
            "cross_1",
            new float[] { -8, -2, 0, 16, 4, 0 },
            new float[][] { null, null, null, null, { 0, 0, 16, 5 }, { 16, 0, 0, 5 } });
        add(
            "cross_2",
            new float[] { -8, 0, -2, 16, 0, 4 },
            new float[][] { null, null, { 0, 0, 16, 5 }, { 16, 0, 0, 5 }, null, null });
    }

    private void add(String name, float[] coordinates, float[][] uv) {
        ModelRenderer part = new ModelRenderer(this);
        part.cubeList.add(
            new QuadBox(part, new CemModel.Box(coordinates, new float[3], new float[2], 32, 32, false, false, uv)));
        parts.put(name, part);
    }

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        return parts;
    }

    public boolean renderArrow(EntityArrow arrow, double x, double y, double z, float partial) {
        CemRuntime.Draw previous = CemRuntime.begin(this, arrow, 0, 0, arrow.ticksExisted + partial, 0, 0);
        if (!CemRuntime.drawing(this)) {
            CemRuntime.end(previous, 1);
            return false;
        }
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(arrow.prevRotationYaw + (arrow.rotationYaw - arrow.prevRotationYaw) * partial - 90, 0, 1, 0);
            GL11.glRotatef(
                arrow.prevRotationPitch + (arrow.rotationPitch - arrow.prevRotationPitch) * partial,
                0,
                0,
                1);
            float shake = arrow.arrowShake - partial;
            if (shake > 0) GL11.glRotatef(-MathHelper.sin(shake * 3) * shake, 0, 0, 1);
            GL11.glRotatef(45, 1, 0, 0);
            GL11.glScalef(0.05625F, 0.05625F, 0.05625F);
            GL11.glTranslatef(-4, 0, 0);
            CemRuntime.recaptureOrigin();
            try {
                for (ModelRenderer part : parts.values()) part.render(1);
            } finally {
                CemRuntime.end(previous, 1);
            }
        } finally {
            GL11.glPopMatrix();
        }
        return true;
    }

    private static final class QuadBox extends ModelBox {

        private final CemBoxMesh mesh;

        QuadBox(ModelRenderer owner, CemModel.Box box) {
            super(owner, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            mesh = new CemBoxMesh(box);
        }

        @Override
        public void render(Tessellator tessellator, float scale) {
            tessellator.startDrawingQuads();
            for (float[] face : mesh.faces) if (face != null) {
                tessellator.setNormal(face[20], face[21], face[22]);
                for (int i = 0; i < 20; i += 5) tessellator.addVertexWithUV(
                    face[i] * scale,
                    face[i + 1] * scale,
                    face[i + 2] * scale,
                    face[i + 3],
                    face[i + 4]);
            }
            tessellator.draw();
        }
    }
}
