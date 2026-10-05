# Additional quaternion rotations

`BoneSnapshot.setAdditionalRotation(x, y, z, w)` accepts a finite, nonzero
quaternion and normalizes it. Invalid input throws without changing the pose.
The overload taking `Quaternionfc` copies its components; the getter writes a
copy into a caller-owned `Quaternionf`. Reset with `resetAdditionalRotation()`.

The additional rotation is in Salamander renderer coordinates, after the existing
bind-plus-pose Euler rotation and before scale:

```
parent * T(offset) * T(pivot) * Rz * Ry * Rx * Qadditional * S * T(-pivot)
```

No axis conversion is applied to this quaternion. A model-format adapter is
responsible for any basis conversion. Existing Euler setters do not reset the
additional rotation. Identity (including a negative identity quaternion) uses the
original Euler-only render path exactly.

`ModelPose.apply` copies the additional rotation for replacement poses and uses
`target * source` for additive poses. With column vectors, the source rotation
acts first. Applications with different track-blending semantics should perform
that blending themselves and supply the resulting quaternion.

Geometry emission and captured bone/locator transforms share this composition.
Hidden bones still have captured matrices, as before. Copies and separate poses
never share mutable quaternion state.

Validation: all 126 tests pass on both standard and Java 8 test tasks. New cases
cover noncommuting rotations, normalization, invalid inputs, extreme finite
components, copy isolation, nested rotated bones, nonuniform scale, normals,
UV preservation, attachment matrices, hidden geometry and exact identity parity.
The synthetic matrix comparisons use a `1e-5` float tolerance.
