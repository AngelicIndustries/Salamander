package industries.angelic.salamander.cem.animation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code nbt(path, test)} calls in loaded animations. Each becomes an input, answered once per entity and frame,
 * so expressions stay plain arithmetic over inputs.
 */
public final class CemNbtQueries {

    public static final class Query {

        public final String path;
        public final String input;
        public final CemNbtTest test;

        private Query(String path, String input, CemNbtTest test) {
            this.path = path;
            this.input = input;
            this.test = test;
        }
    }

    private static final Map<String, Query> QUERIES = new ConcurrentHashMap<>();

    private CemNbtQueries() {}

    /** The input that holds the query's answer, 1 or 0. Throws on a malformed test. */
    public static String register(String path, String instruction) {
        if (path == null || path.trim()
            .isEmpty()) throw new IllegalArgumentException("nbt() needs an NBT path");
        String input = "nbt(" + path.trim() + "," + instruction.trim() + ")";
        if (!QUERIES.containsKey(input)) QUERIES.put(input, new Query(path.trim(), input, CemNbtTest.of(instruction)));
        return input;
    }

    public static Collection<Query> all() {
        return Collections.unmodifiableCollection(QUERIES.values());
    }
}
