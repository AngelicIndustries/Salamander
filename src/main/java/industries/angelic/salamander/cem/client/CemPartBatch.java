package industries.angelic.salamander.cem.client;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import industries.angelic.salamander.cem.model.CemModel;

/**
 * Draws one native part's pack geometry in as few Tessellator draws as possible. Each part's transform is applied on
 * the CPU instead of the GL matrix stack, so consecutive parts with the same texture share a draw. Anything needing
 * GL state of its own flushes first and runs under the GL matrix the parts would have had, keeping the draw order.
 * Render thread only.
 */
final class CemPartBatch {

    /** Column-major 4x4 transforms, as GL keeps them, one per open part. */
    private static double[][] matrices = new double[16][16];
    /** The inverse transpose of each transform's upper 3x3, for normals. */
    private static double[][] normals = new double[16][9];
    private static int depth = -1;
    private static boolean drawing;
    private static ResourceLocation bound;
    private static final FloatBuffer BUFFER = BufferUtils.createFloatBuffer(16);

    private CemPartBatch() {}

    static boolean active() {
        return depth >= 0;
    }

    /** Starts at the current GL matrix. */
    static void begin() {
        depth = 0;
        identity(matrices[0], normals[0]);
        drawing = false;
        bound = null;
    }

    static void end() {
        try {
            flush();
        } finally {
            depth = -1;
            bound = null;
        }
    }

    static void push() {
        if (depth + 1 == matrices.length) {
            matrices = Arrays.copyOf(matrices, matrices.length * 2);
            normals = Arrays.copyOf(normals, normals.length * 2);
            for (int i = depth + 1; i < matrices.length; i++) {
                matrices[i] = new double[16];
                normals[i] = new double[9];
            }
        }
        System.arraycopy(matrices[depth], 0, matrices[depth + 1], 0, 16);
        System.arraycopy(normals[depth], 0, normals[depth + 1], 0, 9);
        depth++;
    }

    static void pop() {
        depth--;
    }

    /** Binds through the texture manager, as the GL path does, when the texture changes; null keeps the bound one. */
    static void texture(ResourceLocation texture) {
        if (texture == null || texture.equals(bound)) return;
        flush();
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        bound = texture;
    }

    /** Something else bound a texture; the next part binds its own again. */
    static void forgetTexture() {
        bound = null;
    }

    /** {@link CemGeometry#transform}, on the CPU. */
    static void transform(CemModel.Node node, double[] pose, float scale, boolean rotationOrder) {
        int offset = node.index * CemModel.STRIDE;
        translate(pose[offset] * scale, pose[offset + 1] * scale, pose[offset + 2] * scale);
        if (rotationOrder) {
            rotate(1, pose[offset + 4]);
            rotate(0, pose[offset + 3]);
            rotate(2, pose[offset + 5]);
        } else {
            rotate(2, pose[offset + 5]);
            rotate(1, pose[offset + 4]);
            rotate(0, pose[offset + 3]);
        }
        scale(pose[offset + 6], pose[offset + 7], pose[offset + 8]);
    }

    static void translate(double x, double y, double z) {
        double[] m = matrices[depth];
        for (int row = 0; row < 4; row++) m[12 + row] += m[row] * x + m[4 + row] * y + m[8 + row] * z;
    }

    /** About axis 0, 1 or 2, by {@code radians}, as {@code glRotated} about that unit axis. */
    private static void rotate(int axis, double radians) {
        if (radians == 0) return;
        double c = Math.cos(radians), s = Math.sin(radians);
        // The two columns the rotation mixes: Y and Z about X, Z and X about Y, X and Y about Z.
        int a = (axis + 1) % 3, b = (axis + 2) % 3;
        double[] m = matrices[depth], n = normals[depth];
        for (int row = 0; row < 4; row++) {
            double first = m[a * 4 + row], second = m[b * 4 + row];
            m[a * 4 + row] = first * c + second * s;
            m[b * 4 + row] = second * c - first * s;
        }
        // A rotation is its own inverse transpose.
        for (int row = 0; row < 3; row++) {
            double first = n[a * 3 + row], second = n[b * 3 + row];
            n[a * 3 + row] = first * c + second * s;
            n[b * 3 + row] = second * c - first * s;
        }
    }

    private static void scale(double x, double y, double z) {
        double[] m = matrices[depth], n = normals[depth];
        double[] factors = { x, y, z };
        for (int column = 0; column < 3; column++) {
            double factor = factors[column];
            for (int row = 0; row < 4; row++) m[column * 4 + row] *= factor;
            // A part flattened to zero still faces its own way, as GL's normalized normals would.
            double inverse = factor == 0 ? 1e9 : 1 / factor;
            for (int row = 0; row < 3; row++) n[column * 3 + row] *= inverse;
        }
    }

    /** Queues faces in the open part's frame: four (x, y, z, u, v) vertices and a normal at [20..22] each. */
    static void add(List<float[]> faces, float scale) {
        if (faces.isEmpty()) return;
        Tessellator tessellator = Tessellator.instance;
        if (!drawing) {
            tessellator.startDrawingQuads();
            drawing = true;
        }
        double[] m = matrices[depth], n = normals[depth];
        for (float[] face : faces) {
            double nx = face[20], ny = face[21], nz = face[22];
            double tx = n[0] * nx + n[3] * ny + n[6] * nz, ty = n[1] * nx + n[4] * ny + n[7] * nz,
                tz = n[2] * nx + n[5] * ny + n[8] * nz;
            double length = Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (length > 0) tessellator.setNormal((float) (tx / length), (float) (ty / length), (float) (tz / length));
            else tessellator.setNormal(0, 0, 0);
            for (int vertex = 0; vertex < 20; vertex += 5) {
                double x = face[vertex] * scale, y = face[vertex + 1] * scale, z = face[vertex + 2] * scale;
                tessellator.addVertexWithUV(
                    m[0] * x + m[4] * y + m[8] * z + m[12],
                    m[1] * x + m[5] * y + m[9] * z + m[13],
                    m[2] * x + m[6] * y + m[10] * z + m[14],
                    face[vertex + 3],
                    face[vertex + 4]);
            }
        }
    }

    static void flush() {
        if (!drawing) return;
        drawing = false;
        Tessellator.instance.draw();
    }

    /** Runs GL drawing of its own under the open part's transform, after what is queued. */
    static void inGl(Runnable drawing) {
        flush();
        double[] m = matrices[depth];
        BUFFER.clear();
        for (double value : m) BUFFER.put((float) value);
        BUFFER.flip();
        GL11.glPushMatrix();
        try {
            GL11.glMultMatrix(BUFFER);
            drawing.run();
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void identity(double[] matrix, double[] normal) {
        Arrays.fill(matrix, 0);
        matrix[0] = matrix[5] = matrix[10] = matrix[15] = 1;
        Arrays.fill(normal, 0);
        normal[0] = normal[4] = normal[8] = 1;
    }
}
