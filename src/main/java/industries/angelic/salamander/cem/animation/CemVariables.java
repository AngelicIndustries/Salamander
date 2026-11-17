package industries.angelic.salamander.cem.animation;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An entity's {@code var.*} and {@code varb.*} values, shared by all of its models. Every name gets one global slot
 * when a model compiles, so evaluation reads and writes arrays; the values still read as a map.
 */
public final class CemVariables extends AbstractMap<String, Double> {

    private static final Map<String, Integer> SLOTS = new ConcurrentHashMap<>();
    private static final List<String> NAMES = new CopyOnWriteArrayList<>();

    private double[] values = new double[16];
    private boolean[] present = new boolean[16];
    private int size;

    /** The slot for a variable name, the same for every model and entity. */
    public static int slot(String name) {
        Integer slot = SLOTS.get(name);
        if (slot != null) return slot;
        synchronized (NAMES) {
            slot = SLOTS.get(name);
            if (slot == null) {
                slot = NAMES.size();
                NAMES.add(name);
                SLOTS.put(name, slot);
            }
            return slot;
        }
    }

    /** An unset variable is 0, as in CEM. */
    public double get(int slot) {
        return slot < values.length ? values[slot] : 0;
    }

    public void set(int slot, double value) {
        if (slot >= values.length) {
            int length = Math.max(slot + 1, values.length * 2);
            values = Arrays.copyOf(values, length);
            present = Arrays.copyOf(present, length);
        }
        values[slot] = value;
        if (!present[slot]) {
            present[slot] = true;
            size++;
        }
    }

    @Override
    public Double get(Object key) {
        Integer slot = SLOTS.get(key);
        return slot == null || slot >= present.length || !present[slot] ? null : values[slot];
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public Double put(String key, Double value) {
        Double previous = get(key);
        set(slot(key), value);
        return previous;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        Arrays.fill(values, 0);
        Arrays.fill(present, false);
        size = 0;
    }

    /** A snapshot, for diagnostics and tests; evaluation never iterates. */
    @Override
    public Set<Entry<String, Double>> entrySet() {
        Map<String, Double> copy = new LinkedHashMap<>();
        for (int slot = 0; slot < present.length; slot++) if (present[slot]) copy.put(NAMES.get(slot), values[slot]);
        return Collections.unmodifiableMap(copy)
            .entrySet();
    }
}
