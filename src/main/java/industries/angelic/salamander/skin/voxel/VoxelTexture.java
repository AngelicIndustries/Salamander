package industries.angelic.salamander.skin.voxel;

public interface VoxelTexture {

    boolean isPresent(VoxelSurfaceBuilder.UV onTextureUV);

    boolean isSolid(VoxelSurfaceBuilder.UV onTextureUV);

    int getWidth();

    int getHeight();

}
