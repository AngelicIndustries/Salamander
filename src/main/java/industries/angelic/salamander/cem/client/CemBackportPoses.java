package industries.angelic.salamander.cem.client;

import java.lang.reflect.Method;

/** Optional EFR pose input, discovered once without loading backport classes on a standalone client. */
public final class CemBackportPoses {

    private static final ClassValue<java.util.Optional<Method>> GLIDING = new ClassValue<java.util.Optional<Method>>() {

        @Override
        protected java.util.Optional<Method> computeValue(Class<?> type) {
            try {
                return java.util.Optional.of(type.getMethod("etfu$isElytraFlying"));
            } catch (NoSuchMethodException ignored) {
                return java.util.Optional.empty();
            }
        }
    };

    private CemBackportPoses() {}

    public static boolean gliding(Object entity) {
        Method method = GLIDING.get(entity.getClass())
            .orElse(null);
        if (method == null) return false;
        try {
            return Boolean.TRUE.equals(method.invoke(entity));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
