package industries.angelic.salamander.cem.model;

import java.util.ArrayList;
import java.util.List;

/** Extrudes a texture rectangle with one edge strip per texel, like Minecraft's held-item geometry. */
public final class CemSpriteMesh {

    public final List<float[]> faces = new ArrayList<>();

    public CemSpriteMesh(CemModel.Box sprite) {
        float[] c = sprite.coordinates, a = sprite.inflation;
        int width = Math.max(1, (int) Math.ceil(c[3])), height = Math.max(1, (int) Math.ceil(c[4]));
        if (width > 4096 || height > 4096) throw new IllegalArgumentException("CEM sprite exceeds 4096 texels");
        float x0 = c[0] - a[0], y0 = c[1] - a[1], z0 = c[2] - a[2];
        float x1 = c[0] + c[3] + a[0], y1 = c[1] + c[4] + a[1], z1 = c[2] + c[5] + a[2];
        float u0 = sprite.uv[0] / sprite.textureWidth, u1 = (sprite.uv[0] + c[3]) / sprite.textureWidth;
        float v0 = sprite.uv[1] / sprite.textureHeight, v1 = (sprite.uv[1] + c[4]) / sprite.textureHeight;
        if (sprite.mirrorU) {
            float t = u0;
            u0 = u1;
            u1 = t;
        }
        if (sprite.mirrorV) {
            float t = v0;
            v0 = v1;
            v1 = t;
        }
        face(new float[] { x1, y0, z0, u1, v0, x0, y0, z0, u0, v0, x0, y1, z0, u0, v1, x1, y1, z0, u1, v1 }, 0, 0, -1);
        face(new float[] { x0, y0, z1, u0, v0, x1, y0, z1, u1, v0, x1, y1, z1, u1, v1, x0, y1, z1, u0, v1 }, 0, 0, 1);
        for (int i = 0; i < width; i++) {
            float left = x0 + (x1 - x0) * i / width, right = x0 + (x1 - x0) * (i + 1) / width;
            float u = u0 + (u1 - u0) * (i + .5f) / width;
            face(
                new float[] { left, y0, z0, u, v0, left, y0, z1, u, v0, left, y1, z1, u, v1, left, y1, z0, u, v1 },
                -1,
                0,
                0);
            face(
                new float[] { right, y0, z1, u, v0, right, y0, z0, u, v0, right, y1, z0, u, v1, right, y1, z1, u, v1 },
                1,
                0,
                0);
        }
        for (int i = 0; i < height; i++) {
            float top = y0 + (y1 - y0) * i / height, bottom = y0 + (y1 - y0) * (i + 1) / height;
            float v = v0 + (v1 - v0) * (i + .5f) / height;
            face(
                new float[] { x1, top, z1, u1, v, x0, top, z1, u0, v, x0, top, z0, u0, v, x1, top, z0, u1, v },
                0,
                -1,
                0);
            face(
                new float[] { x1, bottom, z0, u1, v, x0, bottom, z0, u0, v, x0, bottom, z1, u0, v, x1, bottom, z1, u1,
                    v },
                0,
                1,
                0);
        }
    }

    private void face(float[] vertices, float nx, float ny, float nz) {
        float[] face = new float[23];
        System.arraycopy(vertices, 0, face, 0, 20);
        face[20] = nx;
        face[21] = ny;
        face[22] = nz;
        faces.add(face);
    }
}
