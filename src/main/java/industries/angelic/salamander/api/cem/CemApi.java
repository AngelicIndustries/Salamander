package industries.angelic.salamander.api.cem;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.cem.animation.CemExpressionParser;
import industries.angelic.salamander.cem.animation.CemExtensions;
import industries.angelic.salamander.cem.client.CemModded;
import industries.angelic.salamander.cem.client.CemRuntime;
import industries.angelic.salamander.cem.client.CemTargets;
import industries.angelic.salamander.cem.model.CemModel;

/**
 * Lets other mods extend the resource-pack entity models Salamander draws (OptiFine CEM and EMF), as EMF's animation
 * API does. Without any of it, a mod's living entity already takes the model {@code assets/<modid>/optifine/cem/
 * <entity_name>.jem}, with parts named after its model's fields; Salamander's model exporter writes them out.
 * <p>
 * Register on the client during initialization: models compile when resource packs load, and see only what is
 * registered by then. Draw-time calls arrive on the render thread.
 */
@SideOnly(Side.CLIENT)
public final class CemApi {

    private static final Pattern NAME = Pattern.compile("[a-z_][a-z0-9_]*");

    private CemApi() {}

    /**
     * Draws entities of this exact class with the pack model {@code model}: {@code "mymod:wild_boar"} for
     * {@code assets/mymod/optifine/cem/wild_boar.jem}, or a vanilla name such as {@code "zombie"} to share its packs.
     */
    public static void registerEntity(String entityClass, String model) {
        CemTargets.register(entityClass, model);
    }

    /**
     * Names a model's parts for pack models, in place of the names of its fields: OptiFine's names for a model that
     * copies a vanilla one, say. The map holds the model's own part objects; it is read once per model instance.
     */
    public static void registerParts(Class<? extends ModelBase> modelClass,
        Function<ModelBase, Map<String, ModelRenderer>> parts) {
        CemModded.registerParts(modelClass, parts);
    }

    /**
     * An animation variable named {@code name}, read for the entity or block entity being drawn (possibly null).
     * Lower case letters, digits and underscores, and no name expressions already know.
     */
    public static void registerVariable(String modId, String name, ToDoubleFunction<Object> value) {
        check(modId, name, value);
        if (CemModel.INPUTS.contains(name) || CemModel.RENDER_PROPERTIES.contains(name)
            || CemExpressionParser.builtIn(name)) throw new IllegalArgumentException(name + " is already a CEM name");
        CemExtensions.variable(name, value);
        Salamander.LOG.info("CEM variable '{}' registered by {}", name, modId);
    }

    /**
     * An animation function named {@code name}: a public static method taking doubles and returning a double, called
     * directly from compiled animations.
     */
    public static void registerFunction(String modId, String name, Method method) {
        check(modId, name, method);
        if (CemExpressionParser.builtIn(name) || CemExtensions.isVariable(name))
            throw new IllegalArgumentException(name + " is already a CEM name");
        CemExtensions.function(name, method);
        Salamander.LOG.info("CEM function '{}' registered by {}", name, modId);
    }

    /** The entity or block entity whose model is being drawn now, or null. */
    public static Object currentSubject() {
        return CemRuntime.subject();
    }

    private static void check(String modId, String name, Object value) {
        if (modId == null || value == null
            || name == null
            || !NAME.matcher(name)
                .matches())
            throw new IllegalArgumentException("Invalid CEM registration '" + name + "' from " + modId);
    }
}
