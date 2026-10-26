package industries.angelic.salamander.core;

import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.compat.ears.EarsTransformerAccess;

@SuppressWarnings("unused")
@LateMixin
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins." + Salamander.MODID + ".late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        java.util.List<String> mixins = new java.util.ArrayList<>();
        if (cpw.mods.fml.relauncher.FMLLaunchHandler.side()
            .isClient() && industries.angelic.salamander.config.CemConfig.hooks && loadedMods.contains("etfuturum")) {
            mixins.add("etfuturum.MixinModelFoxCem");
            mixins.add("etfuturum.MixinModelElytraCem");
            mixins.add("etfuturum.MixinBrownMooshroomRendererCem");
            mixins.add("etfuturum.MixinEntityFoxCemInputs");
            mixins.add("etfuturum.MixinEntityBeeCemInputs");
            mixins.add("etfuturum.MixinModelBeeCem");
            mixins.add("etfuturum.MixinModelRabbitCem");
            mixins.add("etfuturum.MixinModelEndermiteCem");
            mixins.add("etfuturum.MixinModelShulkerCem");
            mixins.add("etfuturum.MixinModelShulkerBulletCem");
            mixins.add("etfuturum.MixinModelArmorStandCem");
            mixins.add("etfuturum.MixinModelNewBoatCem");
            mixins.add("etfuturum.MixinChestBoatRendererCem");
            mixins.add("etfuturum.MixinModelBannerCem");
            mixins.add("etfuturum.MixinModelHeadCem");
        }
        if (cpw.mods.fml.relauncher.FMLLaunchHandler.side()
            .isClient() && loadedMods.contains("armoredarms")) {
            mixins.add("armoredarms.MixinArmModelRendererPlayerCem");
            mixins.add("armoredarms.MixinArmModelRendererPlayerSleeve");
        }
        if (cpw.mods.fml.relauncher.FMLLaunchHandler.side()
            .isClient()) {
            // Player skins on other mods' renderers.
            EarsTransformerAccess.open();
            if (loadedMods.contains("etfuturum")) mixins.add("etfuturum.MixinTileEntityFancySkullRenderer");
            if (loadedMods.contains("aether_legacy")) {
                mixins.add("aether.MixinAetherItemRenderer");
                mixins.add("aether.MixinPlayerAetherRenderer");
            }
            if (loadedMods.contains("ears") || loadedMods.contains("foamfix"))
                mixins.add("ears.MixinLayerEarsDelegate");
            if (loadedMods.contains("Botania")) mixins.add("botania.MixinClientProxyPlayerModel");
            if (loadedMods.contains("Avaritia")) mixins.add("avaritia.MixinModelArmorInfinity");
            if (loadedMods.contains("modelcitizens")) mixins.add("modelcitizens.MixinRenderCorpse");
            if (loadedMods.contains("SmartRender")) mixins.add("smartrender.MixinSmartRenderModelPlayer");
        }
        return mixins;
    }
}
