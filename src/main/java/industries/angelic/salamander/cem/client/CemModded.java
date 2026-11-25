package industries.angelic.salamander.cem.client;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import industries.angelic.salamander.Salamander;

/**
 * Other mods' entities, as EMF finds them: a model named {@code <modid>:<entity_name>} from the mod's registration with
 * Forge, and parts named after the model's own fields, since 1.7.10 models have no part names of their own.
 */
public final class CemModded {

    private static final Map<Class<?>, String> TARGETS = new HashMap<>();
    private static final Map<Class<?>, Function<ModelBase, Map<String, ModelRenderer>>> PARTS = new HashMap<>();
    private static final Map<Class<?>, List<Field>> FIELDS = new HashMap<>();

    private CemModded() {}

    /** Names a mod's model parts for pack models, in place of the names of its fields. */
    public static void registerParts(Class<? extends ModelBase> model,
        Function<ModelBase, Map<String, ModelRenderer>> parts) {
        PARTS.put(model, parts);
    }

    /** {@code <modid>:<entity_name>} for an entity a mod registered with Forge, else null. */
    static String target(Object subject) {
        if (!(subject instanceof Entity)) return null;
        Class<?> type = subject.getClass();
        if (!TARGETS.containsKey(type)) TARGETS.put(type, lookup(type));
        return TARGETS.get(type);
    }

    @SuppressWarnings("unchecked")
    private static String lookup(Class<?> type) {
        try {
            cpw.mods.fml.common.registry.EntityRegistry.EntityRegistration registration = cpw.mods.fml.common.registry.EntityRegistry
                .instance()
                .lookupModSpawn((Class<? extends Entity>) type, false);
            if (registration == null) return null;
            String mod = name(
                registration.getContainer()
                    .getModId());
            String entity = name(registration.getEntityName());
            return mod.isEmpty() || entity.isEmpty() ? null : mod + ":" + entity;
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }

    /**
     * A name as resource paths and pack models spell it: lower snake case. {@code WildBoar} is {@code wild_boar},
     * {@code TFKobold} is {@code tf_kobold}, {@code mob.Ghoul} is {@code mob_ghoul}.
     */
    public static String name(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!Character.isLetterOrDigit(ch) || ch > 127) {
                if (result.length() > 0 && result.charAt(result.length() - 1) != '_') result.append('_');
                continue;
            }
            if (Character.isUpperCase(ch) && result.length() > 0 && result.charAt(result.length() - 1) != '_') {
                char previous = text.charAt(i - 1);
                boolean nextLower = i + 1 < text.length() && Character.isLowerCase(text.charAt(i + 1));
                if (Character.isLowerCase(previous) || Character.isDigit(previous)
                    || Character.isUpperCase(previous) && nextLower) result.append('_');
            }
            result.append(Character.toLowerCase(ch));
        }
        int end = result.length();
        while (end > 0 && result.charAt(end - 1) == '_') end--;
        return result.substring(0, end)
            .toLowerCase(Locale.ROOT);
    }

    /** Whether CEM can bind this model, as {@link #parts} would, without collecting its parts. */
    static boolean bindable(ModelBase model, Object subject) {
        return model instanceof CemModelParts || provider(model.getClass()) != null
            || !CemTargets.builtIn(subject) && CemTargets.target(subject) != null;
    }

    /**
     * The parts of a model by the names pack models use, or null for a model CEM cannot bind: its own names, a
     * registered provider's, and for another mod's model drawing a modded entity, the names of its fields.
     */
    static Map<String, ModelRenderer> parts(ModelBase model, Object subject) {
        Function<ModelBase, Map<String, ModelRenderer>> provider = provider(model.getClass());
        if (provider != null) return provider.apply(model);
        boolean modded = !CemTargets.builtIn(subject) && CemTargets.target(subject) != null;
        if (model instanceof CemModelParts) {
            Map<String, ModelRenderer> parts = ((CemModelParts) model).salamander$cemParts();
            // A mod's model extending a vanilla one keeps the vanilla names and adds its own parts.
            if (!modded || model.getClass()
                .getName()
                .startsWith("net.minecraft.")) return parts;
            Map<String, ModelRenderer> merged = new LinkedHashMap<>(parts);
            Set<ModelRenderer> named = Collections.newSetFromMap(new IdentityHashMap<>());
            named.addAll(parts.values());
            for (Map.Entry<String, ModelRenderer> extra : discover(model).entrySet())
                if (!named.contains(extra.getValue()) && !merged.containsKey(extra.getKey()))
                    merged.put(extra.getKey(), extra.getValue());
            return merged;
        }
        return modded ? discover(model) : null;
    }

    private static Function<ModelBase, Map<String, ModelRenderer>> provider(Class<?> type) {
        for (Class<?> current = type; current != null
            && current != ModelBase.class; current = current.getSuperclass()) {
            Function<ModelBase, Map<String, ModelRenderer>> provider = PARTS.get(current);
            if (provider != null) return provider;
        }
        return null;
    }

    /**
     * Parts named after the model's fields, the superclass's first: a field {@code leftWing} is {@code left_wing}, an
     * array or list {@code legs} is {@code legs1}, {@code legs2}... A part no field holds is {@code part<N>}, by its
     * place in the model's part list.
     */
    public static Map<String, ModelRenderer> discover(ModelBase model) {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        Set<ModelRenderer> named = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Field field : fields(model.getClass())) {
            Object value;
            try {
                value = field.get(model);
            } catch (IllegalAccessException exception) {
                continue;
            }
            String name = name(field.getName());
            if (value instanceof ModelRenderer) put(parts, named, name, (ModelRenderer) value);
            else if (value instanceof ModelRenderer[]) {
                ModelRenderer[] array = (ModelRenderer[]) value;
                for (int i = 0; i < array.length; i++) put(parts, named, name + (i + 1), array[i]);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) if (list.get(i) instanceof ModelRenderer)
                    put(parts, named, name + (i + 1), (ModelRenderer) list.get(i));
            }
        }
        List<?> boxes = model.boxList;
        if (boxes != null) for (int i = 0; i < boxes.size(); i++) if (boxes.get(i) instanceof ModelRenderer)
            put(parts, named, "part" + (i + 1), (ModelRenderer) boxes.get(i));
        return parts;
    }

    private static void put(Map<String, ModelRenderer> parts, Set<ModelRenderer> named, String name,
        ModelRenderer part) {
        if (part == null || named.contains(part) || name.isEmpty()) return;
        // CEM keeps "root" for the whole model.
        if (name.equals("root")) name = "root_part";
        while (parts.containsKey(name)) name += "_";
        parts.put(name, part);
        named.add(part);
    }

    private static List<Field> fields(Class<?> type) {
        List<Field> fields = FIELDS.get(type);
        if (fields != null) return fields;
        List<Class<?>> chain = new ArrayList<>();
        for (Class<?> current = type; current != null && current != ModelBase.class
            && current != Object.class; current = current.getSuperclass()) chain.add(0, current);
        fields = new ArrayList<>();
        for (Class<?> current : chain) for (Field field : current.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            Class<?> kind = field.getType();
            if (!ModelRenderer.class.isAssignableFrom(kind) && !ModelRenderer[].class.isAssignableFrom(kind)
                && !List.class.isAssignableFrom(kind)) continue;
            try {
                field.setAccessible(true);
                fields.add(field);
            } catch (RuntimeException exception) {
                Salamander.LOG.debug("CEM cannot read {}.{}", current.getName(), field.getName());
            }
        }
        FIELDS.put(type, fields);
        return fields;
    }
}
