package industries.angelic.salamander.cem.animation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.objectweb.asm.Type;

/**
 * Animation variables and functions other mods add, as through EMF's animation API. Expressions see them by name
 * once a model compiles: register them before resource packs load.
 */
public final class CemExtensions {

    private static final Map<String, ToDoubleFunction<Object>> VARIABLES = new LinkedHashMap<>();
    /** What every model evaluation reads: a copy made on registration, never during drawing. */
    private static volatile Map<String, ToDoubleFunction<Object>> variables = Collections.emptyMap();
    private static final Map<String, Function> FUNCTIONS = Collections.synchronizedMap(new LinkedHashMap<>());

    private CemExtensions() {}

    /** A public static method of {@code double} arguments returning a {@code double}, called by name. */
    public static final class Function {

        public final String name;
        public final Method method;
        public final int arity;
        /** The method as the bytecode compiler calls it. */
        final String owner, descriptor;

        Function(String name, Method method) {
            this.name = name;
            this.method = method;
            this.arity = method.getParameterTypes().length;
            this.owner = Type.getInternalName(method.getDeclaringClass());
            this.descriptor = Type.getMethodDescriptor(method);
        }

        double call(double[] arguments) {
            Object[] boxed = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) boxed[i] = arguments[i];
            try {
                return (Double) method.invoke(null, boxed);
            } catch (ReflectiveOperationException exception) {
                return Double.NaN;
            }
        }
    }

    /** A variable whose value {@code value} reads from the entity or block entity being drawn, or null. */
    public static synchronized void variable(String name, ToDoubleFunction<Object> value) {
        VARIABLES.put(name, value);
        variables = Collections.unmodifiableMap(new LinkedHashMap<>(VARIABLES));
    }

    public static boolean isVariable(String name) {
        return variables.containsKey(name);
    }

    /** The registered variables, in the order they were registered. */
    public static Map<String, ToDoubleFunction<Object>> variables() {
        return variables;
    }

    public static void function(String name, Method method) {
        if (!Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())
            || !Modifier.isPublic(
                method.getDeclaringClass()
                    .getModifiers()))
            throw new IllegalArgumentException(
                name + ": " + method + " is not a public static method of a public class");
        if (method.getReturnType() != double.class)
            throw new IllegalArgumentException(name + ": " + method + " does not return a double");
        for (Class<?> parameter : method.getParameterTypes()) if (parameter != double.class)
            throw new IllegalArgumentException(name + ": " + method + " takes something other than doubles");
        FUNCTIONS.put(name, new Function(name, method));
    }

    /** The registered function of this name, or null. */
    public static Function function(String name) {
        return FUNCTIONS.get(name);
    }
}
