package industries.angelic.salamander.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SkinLayersConfigMigrationTest {

    @Test
    public void splitsWawelAuthsSettingsIntoSalamandersCategories() {
        String wawelAuth = "general {\n    B:debugMode=false\n}\n\n" + "skinlayers3d {\n"
            + "    # Master toggle\n"
            + "    B:enabled3D=false\n"
            + "    B:enableHat3D=false\n"
            + "    S:skullVoxelSize=1.3\n"
            + "    B:fastRender=true\n"
            + "    B:translucentBase=false\n"
            + "    I:renderDistanceLOD=20\n"
            + "}\n";
        assertEquals(
            "skins {\n" + "    B:translucentBase=false\n"
                + "}\n"
                + "\nskinlayers3d {\n"
                + "    B:enabled3D=false\n"
                + "    I:renderDistanceLOD=20\n"
                + "\n    layers {\n"
                + "        B:enableHat3D=false\n"
                + "    }\n"
                + "}\n"
                + "\nskulls {\n"
                + "    S:skullVoxelSize=1.3\n"
                + "}\n",
            SkinLayersConfigMigration.split(wawelAuth));
    }

    @Test
    public void nothingWithoutKnownSettings() {
        assertNull(SkinLayersConfigMigration.split("skinlayers3d {\n    B:fastRender=true\n}\n"));
    }

    @Test
    public void nothingWithoutTheCategory() {
        assertNull(SkinLayersConfigMigration.split("general {\n    B:debugMode=false\n}\n"));
    }
}
