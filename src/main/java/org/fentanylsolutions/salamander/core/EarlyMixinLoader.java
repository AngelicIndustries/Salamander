package org.fentanylsolutions.salamander.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fentanylsolutions.salamander.Salamander;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@SuppressWarnings("unused")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixinLoader implements IEarlyMixinLoader, IFMLLoadingPlugin {

    @Override
    public String getMixinConfig() {
        return "mixins." + Salamander.MODID + ".early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        List<String> mixins = new ArrayList<>();
        mixins.add("minecraft.MixinEntityMobCem");

        if (FMLLaunchHandler.side()
            .isClient()) {
            mixins.add("minecraft.client.AccessorMinecraft");
            mixins.add("minecraft.client.renderer.MixinImageBufferDownloadCem");
            mixins.add("minecraft.client.renderer.MixinThreadDownloadImageDataCem");
            mixins.add("minecraft.client.renderer.texture.MixinDynamicTextureCem");
            mixins.add("minecraft.client.resources.MixinSkinManagerCem");
            mixins.add("minecraft.client.renderer.MixinRenderBlocksCemBed");
            mixins.add("minecraft.client.renderer.MixinRenderBlocksCemMushrooms");
            mixins.add("minecraft.client.renderer.entity.MixinRenderMooshroomCem");
            mixins.add("minecraft.client.renderer.MixinRenderGlobalCemBeds");
            mixins.add("minecraft.client.AccessorCemNbtList");
            mixins.add("minecraft.client.MixinEntityArrowCemInputs");
            mixins.add("minecraft.client.MixinEntitySquidCemInputs");
            mixins.add("minecraft.client.MixinEntitySilverfishCemInputs");
            mixins.add("minecraft.client.MixinEntityLivingBaseCemHeadRotation");
            mixins.add("minecraft.client.MixinNetHandlerPlayClientCemHeadRotation");
            mixins.add("minecraft.client.resources.AccessorAbstractResourcePack");
            mixins.add("minecraft.client.renderer.entity.MixinRenderPlayer");
            mixins.add("minecraft.client.renderer.entity.MixinRenderSpiderCem");
            mixins.add("minecraft.client.model.MixinModelIronGolemCemRender");
            mixins.add("minecraft.client.model.MixinModelSilverfishCemRender");
            mixins.add("minecraft.client.model.MixinModelMinecartCemRender");
            mixins.add("minecraft.client.model.MixinModelBatCemRender");
            mixins.add("minecraft.client.model.MixinModelQuadrupedCemRender");
            mixins.add("minecraft.client.model.MixinModelBookCemRender");
            mixins.add("minecraft.client.model.MixinModelWitherCemRender");
            mixins.add("minecraft.client.model.MixinModelVillagerCemRender");
            mixins.add("minecraft.client.model.MixinModelMagmaCubeCemRender");
            mixins.add("minecraft.client.model.MixinModelBlazeCemRender");
            mixins.add("minecraft.client.model.MixinModelSnowManCemRender");
            mixins.add("minecraft.client.model.MixinModelSpiderCemRender");
            mixins.add("minecraft.client.model.MixinModelCreeperCemRender");
            mixins.add("minecraft.client.model.MixinModelEnderCrystalCemRender");
            mixins.add("minecraft.client.model.MixinModelBoatCemRender");
            mixins.add("minecraft.client.model.MixinModelLeashKnotCemRender");
            mixins.add("minecraft.client.model.MixinModelOcelotCemRender");
            mixins.add("minecraft.client.model.MixinModelSquidCemRender");
            mixins.add("minecraft.client.model.MixinModelHorseCemRender");
            mixins.add("minecraft.client.model.MixinModelDragonCemRender");
            mixins.add("minecraft.client.model.MixinModelWolfCemRender");
            mixins.add("minecraft.client.model.MixinModelSlimeCemRender");
            mixins.add("minecraft.client.model.MixinModelBipedCemRender");
            mixins.add("minecraft.client.model.MixinModelChickenCemRender");
            mixins.add("minecraft.client.model.MixinModelGhastCemRender");
            mixins.add("minecraft.client.model.MixinModelSkeletonHeadCemRender");
            mixins.add("minecraft.client.model.MixinModelChestCemRender");
            mixins.add("minecraft.client.model.MixinModelSignCemRender");
            mixins.add("minecraft.client.model.MixinModelSpiderCemParts");
            mixins.add("minecraft.client.model.MixinModelDragonCemParts");
            mixins.add("minecraft.client.model.MixinModelBipedCemParts");
            mixins.add("minecraft.client.model.MixinModelQuadrupedCemParts");
            mixins.add("minecraft.client.model.MixinModelCreeperCemParts");
            mixins.add("minecraft.client.model.MixinModelChickenCemParts");
            mixins.add("minecraft.client.model.MixinModelBatCemParts");
            mixins.add("minecraft.client.model.MixinModelBlazeCemParts");
            mixins.add("minecraft.client.model.MixinModelGhastCemParts");
            mixins.add("minecraft.client.model.MixinModelSquidCemParts");
            mixins.add("minecraft.client.model.MixinModelMagmaCubeCemParts");
            mixins.add("minecraft.client.model.MixinModelSilverfishCemParts");
            mixins.add("minecraft.client.model.MixinModelSlimeCemParts");
            mixins.add("minecraft.client.model.MixinModelIronGolemCemParts");
            mixins.add("minecraft.client.model.MixinModelSnowManCemParts");
            mixins.add("minecraft.client.model.MixinModelVillagerCemParts");
            mixins.add("minecraft.client.model.MixinModelWitchCemParts");
            mixins.add("minecraft.client.model.MixinModelWolfCemParts");
            mixins.add("minecraft.client.model.MixinModelOcelotCemParts");
            mixins.add("minecraft.client.model.MixinModelWitherCemParts");
            mixins.add("minecraft.client.model.MixinModelBoatCemParts");
            mixins.add("minecraft.client.model.MixinModelMinecartCemParts");
            mixins.add("minecraft.client.model.MixinModelEnderCrystalCemParts");
            mixins.add("minecraft.client.model.MixinModelLeashKnotCemParts");
            mixins.add("minecraft.client.model.MixinModelChestCemParts");
            mixins.add("minecraft.client.model.MixinModelSignCemParts");
            mixins.add("minecraft.client.model.MixinModelSkeletonHeadCemParts");
            mixins.add("minecraft.client.model.MixinModelBookCemParts");
            mixins.add("minecraft.client.model.MixinModelHorseCemParts");
            mixins.add("minecraft.client.model.MixinModelRendererCem");
            mixins.add("minecraft.client.renderer.entity.MixinRenderManagerCem");
            mixins.add("minecraft.client.renderer.entity.MixinRenderItemCem");
            mixins.add("minecraft.client.renderer.MixinItemRendererCem");
            mixins.add("minecraft.client.renderer.entity.MixinRenderLivingCemLeash");
            mixins.add("minecraft.client.renderer.entity.MixinRenderArrowCem");
            mixins.add("minecraft.client.renderer.entity.MixinRenderCemProperties");
            mixins.add("minecraft.client.renderer.tileentity.MixinTileEntityRendererDispatcherCem");
            mixins.add("minecraft.client.renderer.texture.MixinTextureManagerCem");
            mixins.add("minecraft.client.resources.MixinPackMetadataSectionSerializer");
            mixins.add("minecraft.client.resources.MixinReloadableCemPacks");
        }

        return mixins;
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
