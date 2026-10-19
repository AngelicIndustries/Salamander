package industries.angelic.salamander.cem.client;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import industries.angelic.salamander.cem.model.CemModel;

/** Inserts the model root before native baby/pose transforms instead of applying it separately in each bone's axes. */
public final class CemRenderFrame {

    private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
    private Matrix4f origin, inverse;
    private final boolean deferred;

    public CemRenderFrame(boolean deferred) {
        this.deferred = deferred;
        if (!deferred) capture();
    }

    public void recapture() {
        origin = null;
        capture();
    }

    public boolean deferred() {
        return deferred;
    }

    public void capture() {
        if (origin != null) return;
        buffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
        origin = new Matrix4f(buffer);
        inverse = new Matrix4f(origin).invert();
    }

    private Matrix4f root(CemModel.Node node, double[] pose, float scale) {
        capture();
        int offset = node.index * CemModel.STRIDE;
        return new Matrix4f(origin)
            .translate((float) pose[offset] * scale, (float) pose[offset + 1] * scale, (float) pose[offset + 2] * scale)
            .rotateZ((float) pose[offset + 5])
            .rotateY((float) pose[offset + 4])
            .rotateX((float) pose[offset + 3])
            .scale((float) pose[offset + 6], (float) pose[offset + 7], (float) pose[offset + 8]);
    }

    public void insertRoot(CemModel.Node node, double[] pose, float scale) {
        int offset = node.index * CemModel.STRIDE;
        if (pose[offset] == 0 && pose[offset + 1] == 0
            && pose[offset + 2] == 0
            && pose[offset + 3] == 0
            && pose[offset + 4] == 0
            && pose[offset + 5] == 0
            && pose[offset + 6] == 1
            && pose[offset + 7] == 1
            && pose[offset + 8] == 1) return;
        Matrix4f transform = root(node, pose, scale).mul(inverse);
        buffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
        transform.mul(new Matrix4f(buffer));
        buffer.clear();
        transform.get(buffer);
        GL11.glLoadMatrix(buffer);
    }

    public void atOrigin(CemModel.Node node, double[] pose, float scale) {
        Matrix4f transform = root(node, pose, scale);
        buffer.clear();
        transform.get(buffer);
        GL11.glLoadMatrix(buffer);
    }
}
