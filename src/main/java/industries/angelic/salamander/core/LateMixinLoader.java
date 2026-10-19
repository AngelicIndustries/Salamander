package industries.angelic.salamander.core;

import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import industries.angelic.salamander.Salamander;

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
            .isClient() && loadedMods.contains("etfuturum")) {
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
        return mixins;
    }
}
