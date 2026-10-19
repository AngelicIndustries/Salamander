package industries.angelic.salamander.cem.client;

/** Scoped item flags shared by GUI and held block/skull renderers. */
public final class CemItemContext {

    private static int mode;

    private CemItemContext() {}

    public static int enter(int value) {
        int previous = mode;
        mode = value;
        return previous;
    }

    public static void leave(int previous) {
        mode = previous;
    }

    public static boolean hand() {
        return mode == 1;
    }

    public static boolean gui() {
        return mode == 2;
    }
}
