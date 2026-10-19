package industries.angelic.salamander.cem.model;

/** Bakes native Minecraft box winding/UV layout with fractional dimensions and independently mirrored axes. */
public final class CemBoxMesh {

    /** Six faces; each face contains four vertices (x,y,z,u,v) and a unit normal. */
    public final float[][] faces = new float[6][23];

    public CemBoxMesh(CemModel.Box box) {
        float[] c = box.coordinates;
        float[] add = box.inflation;
        float x0 = c[0] - add[0], y0 = c[1] - add[1], z0 = c[2] - add[2];
        float x1 = c[0] + c[3] + add[0], y1 = c[1] + c[4] + add[1], z1 = c[2] + c[5] + add[2];
        if (box.mirrorU) {
            float temp = x0;
            x0 = x1;
            x1 = temp;
        }
        boolean reflectY = box.faceUvs != null && box.mirrorV;
        if (reflectY) {
            float temp = y0;
            y0 = y1;
            y1 = temp;
        }
        float[][] vertices = { { x0, y0, z0 }, { x1, y0, z0 }, { x1, y1, z0 }, { x0, y1, z0 }, { x0, y0, z1 },
            { x1, y0, z1 }, { x1, y1, z1 }, { x0, y1, z1 } };
        int[][] indices = { { 5, 1, 2, 6 }, { 0, 4, 7, 3 }, { 5, 4, 0, 1 }, { 2, 3, 7, 6 }, { 1, 0, 3, 2 },
            { 4, 5, 6, 7 } };
        if (box.faceUvs != null) {
            // Individual top/bottom UV rectangles start at the opposite corner to box-UV unfolding.
            indices[2] = new int[] { 0, 1, 5, 4 };
            indices[3] = new int[] { 7, 6, 2, 3 };
        }
        float u = box.uv[0], v = box.uv[1], w = c[3], h = c[4], d = c[5];
        float[][] uvs = { { u + d + w, v + d, u + d + w + d, v + d + h }, { u, v + d, u + d, v + d + h },
            { u + d, v, u + d + w, v + d }, { u + d + w, v + d, u + d + w + w, v },
            { u + d, v + d, u + d + w, v + d + h }, { u + d + w + d, v + d, u + d + w + d + w, v + d + h } };
        float[][] normals = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 } };
        for (int face = 0; face < 6; face++) {
            if (box.faceUvs != null && box.faceUvs[face] == null) {
                faces[face] = null;
                continue;
            }
            float[] uv = box.faceUvs == null ? uvs[face] : box.faceUvs[face].clone();
            if (box.mirrorV && !reflectY) {
                float temp = uv[1];
                uv[1] = uv[3];
                uv[3] = temp;
            }
            for (int vertex = 0; vertex < 4; vertex++) {
                int out = (box.mirrorU != reflectY ? 3 - vertex : vertex) * 5;
                float[] position = vertices[indices[face][vertex]];
                System.arraycopy(position, 0, faces[face], out, 3);
                faces[face][out + 3] = (vertex == 0 || vertex == 3 ? uv[2] : uv[0]) / box.textureWidth;
                faces[face][out + 4] = (vertex < 2 ? uv[1] : uv[3]) / box.textureHeight;
            }
            float[] f = faces[face];
            float ax = f[5] - f[0], ay = f[6] - f[1], az = f[7] - f[2], bx = f[10] - f[0], by = f[11] - f[1],
                bz = f[12] - f[2];
            float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 0) {
                f[20] = nx / length;
                f[21] = ny / length;
                f[22] = nz / length;
            } else for (int axis = 0; axis < 3; axis++)
                f[20 + axis] = normals[face][axis] * (axis == 0 && box.mirrorU || axis == 1 && reflectY ? -1 : 1);
        }
    }
}
