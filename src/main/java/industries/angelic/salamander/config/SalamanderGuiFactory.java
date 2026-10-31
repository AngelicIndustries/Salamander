package industries.angelic.salamander.config;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;

import industries.angelic.salamander.Salamander;

/** Salamander's settings in the mod list: a button per category, the skin ones first. */
public class SalamanderGuiFactory implements SimpleGuiFactory {

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigGui.class;
    }

    public static class ConfigGui extends SimpleGuiConfig {

        public ConfigGui(GuiScreen parent) throws ConfigException {
            super(
                parent,
                Salamander.MODID,
                Salamander.MODNAME,
                true,
                SkinConfig.class,
                SkinLayers3DConfig.class,
                SkullConfig.class,
                CemConfig.class,
                TextureConfig.class,
                DebugConfig.class);
        }
    }
}
