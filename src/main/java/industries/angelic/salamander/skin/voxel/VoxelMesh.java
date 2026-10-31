package industries.angelic.salamander.skin.voxel;

import java.util.List;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

/**
 * Extends VoxelModelPart for 1.7.10 GL rendering.
 * <p>
 * Compiles polygon data into a GL display list for fast repeated rendering.
 */
public class VoxelMesh extends VoxelModelPart {

    private int displayList = -1;

    public VoxelMesh(List<VoxelCube> customCubes) {
        super(customCubes);
    }

    /**
     * Compile the polygon data into a GL display list. Must be called on the render
     * thread after construction.
     */
    public void compileDisplayList() {
        if (displayList != -1) {
            GL11.glDeleteLists(displayList, 1);
        }
        displayList = GL11.glGenLists(1);
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        paint(polygonData);
        GL11.glEndList();
    }

    private static void paint(float[] data) {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        for (int id = 0; id < data.length; id += 23) {
            tess.setNormal(data[id], data[id + 1], data[id + 2]);
            for (int v = 0; v < 4; v++) {
                int base = id + 3 + (v * 5);
                tess.addVertexWithUV(data[base], data[base + 1], data[base + 2], data[base + 3], data[base + 4]);
            }
        }
        tess.draw();
    }

    /**
     * Render this mesh with the given scale and voxel size.
     * The skin texture must already be bound before calling this.
     *
     * @param scale     model scale factor (typically 0.0625 = 1/16)
     * @param voxelSize voxel size multiplier (e.g. 1.15 for body, 1.18 for head)
     */
    public void render(float scale, float voxelSize) {
        render(scale, voxelSize, voxelSize, voxelSize, 0.0F, 0.0F, 0.0F);
    }

    /**
     * Render this mesh with per-axis scaling and local translation offsets.
     * Offsets are in model-space units (pixels), applied before rotation/scaling.
     */
    public void render(float scale, float scaleX, float scaleY, float scaleZ, float localOffsetX, float localOffsetY,
        float localOffsetZ) {
        if (displayList == -1 || !visible) return;
        GL11.glPushMatrix();
        // Translate to rotation point (matching ModelRenderer convention).
        GL11.glTranslatef(
            (x + offsetX + localOffsetX) * scale,
            (y + offsetY + localOffsetY) * scale,
            (z + offsetZ + localOffsetZ) * scale);
        // Apply rotations (ZYX order, matching ModelRenderer).
        if (zRot != 0) GL11.glRotatef(zRot * (180F / (float) Math.PI), 0, 0, 1);
        if (yRot != 0) GL11.glRotatef(yRot * (180F / (float) Math.PI), 0, 1, 0);
        if (xRot != 0) GL11.glRotatef(xRot * (180F / (float) Math.PI), 1, 0, 0);
        // Vertex positions are already pre-scaled to 1/16 units (scaledX = pos.x / 16).
        GL11.glScalef(scaleX, scaleY, scaleZ);
        GL11.glCallList(displayList);
        GL11.glPopMatrix();
    }

    /** Calls the compiled list under the current transform, ignoring this part's own position and rotation. */
    public void draw() {
        if (displayList != -1) GL11.glCallList(displayList);
    }

    /**
     * Delete the GL display list. Call when the mesh is no longer needed.
     */
    public void cleanup() {
        if (displayList != -1) {
            GL11.glDeleteLists(displayList, 1);
            displayList = -1;
        }
    }

    /**
     * @return true if the display list has been compiled and is ready for rendering
     */
    public boolean isCompiled() {
        return displayList != -1;
    }
}
