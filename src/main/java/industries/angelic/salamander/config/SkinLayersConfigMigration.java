package industries.angelic.salamander.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.salamander.Salamander;

/**
 * Wawel Auth kept the skin layer settings before Salamander drew the layers. They are carried over once, before
 * Salamander's config is read, if Salamander has none of its own yet, each into the category it now sits in.
 */
public final class SkinLayersConfigMigration {

    private static final String CATEGORY = "skinlayers3d {";
    /** Where Wawel Auth's skin layer settings went. */
    private static final Class<?>[] CONFIGS = { SkinConfig.class, SkinLayers3DConfig.class, SkullConfig.class };

    private SkinLayersConfigMigration() {}

    public static void migrate(File configDir) {
        File from = new File(configDir, "wawelauth/wawelauth.cfg");
        File to = new File(configDir, Salamander.MODID + "/" + Salamander.MODID + ".cfg");
        if (!from.isFile()) return;
        try {
            String target = to.isFile() ? read(to) : "";
            if (target.contains(CATEGORY)) return;
            String block = split(read(from));
            if (block == null) return;
            String separator = target.isEmpty() || target.endsWith("\n\n") ? "" : target.endsWith("\n") ? "\n" : "\n\n";
            File dir = to.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return;
            Files.write(to.toPath(), (target + separator + block).getBytes(StandardCharsets.UTF_8));
            Salamander.LOG.info("Carried the skin layer settings over from Wawel Auth's config");
        } catch (IOException | RuntimeException e) {
            Salamander.LOG.warn("Could not carry the skin layer settings over from Wawel Auth's config", e);
        }
    }

    /**
     * Wawel Auth's skin layer category in {@code source}, as Salamander's categories: each setting Salamander still
     * has, in the category or subcategory it sits in. Null if there are none.
     */
    static String split(String source) {
        int start = source.indexOf(CATEGORY);
        int end = start < 0 ? -1 : source.indexOf("\n}", start);
        if (end < 0) return null;
        // Each setting's category and subcategory ("" for none), and what goes in each, in the configs' order.
        Map<String, String[]> homes = new LinkedHashMap<>();
        Map<String, Map<String, List<String>>> categories = new LinkedHashMap<>();
        for (Class<?> config : CONFIGS) {
            String category = config.getAnnotation(Config.class)
                .category()
                .toLowerCase(Locale.ROOT);
            Map<String, List<String>> subcategories = new LinkedHashMap<>();
            subcategories.put("", new ArrayList<>());
            categories.put(category, subcategories);
            for (Field field : config.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())) continue;
                if (field.getType()
                    .isPrimitive()) {
                    homes.put(field.getName(), new String[] { category, "" });
                    continue;
                }
                String subcategory = field.getName()
                    .toLowerCase(Locale.ROOT);
                subcategories.put(subcategory, new ArrayList<>());
                for (Field setting : field.getType()
                    .getDeclaredFields())
                    if (!Modifier.isStatic(setting.getModifiers()) && Modifier.isPublic(setting.getModifiers()))
                        homes.put(setting.getName(), new String[] { category, subcategory });
            }
        }
        for (String line : source.substring(start + CATEGORY.length(), end)
            .split("\n")) {
            String setting = line.trim();
            int type = setting.indexOf(':'), equals = setting.indexOf('=');
            if (setting.startsWith("#") || type < 0 || equals < type) continue;
            String[] home = homes.get(setting.substring(type + 1, equals));
            if (home != null) categories.get(home[0])
                .get(home[1])
                .add(setting);
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Map<String, List<String>>> category : categories.entrySet()) {
            StringBuilder block = new StringBuilder();
            for (Map.Entry<String, List<String>> subcategory : category.getValue()
                .entrySet()) {
                if (subcategory.getValue()
                    .isEmpty()) continue;
                boolean nested = !subcategory.getKey()
                    .isEmpty();
                if (nested) block.append("\n    ")
                    .append(subcategory.getKey())
                    .append(" {\n");
                for (String setting : subcategory.getValue()) block.append(nested ? "        " : "    ")
                    .append(setting)
                    .append('\n');
                if (nested) block.append("    }\n");
            }
            if (block.length() == 0) continue;
            if (out.length() > 0) out.append('\n');
            out.append(category.getKey())
                .append(" {\n")
                .append(block)
                .append("}\n");
        }
        return out.length() == 0 ? null : out.toString();
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
