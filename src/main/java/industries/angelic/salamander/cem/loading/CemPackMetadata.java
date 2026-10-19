package industries.angelic.salamander.cem.loading;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Allows modern pack metadata to pass the legacy metadata reader without rewriting the pack ZIP. */
public final class CemPackMetadata {

    private CemPackMetadata() {}

    public static JsonElement legacyCompatible(JsonElement element) {
        if (!element.isJsonObject()) return element;
        JsonObject original = element.getAsJsonObject();
        if (original.has("pack_format") || !original.has("min_format")) return element;
        JsonElement minimum = original.get("min_format");
        JsonElement major = minimum.isJsonArray() && minimum.getAsJsonArray()
            .size() > 0 ? minimum.getAsJsonArray()
                .get(0) : minimum;
        if (!major.isJsonPrimitive() || !major.getAsJsonPrimitive()
            .isNumber() || major.getAsDouble() < 1) return element;
        JsonObject copy = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : original.entrySet()) copy.add(entry.getKey(), entry.getValue());
        copy.addProperty("pack_format", 1);
        return copy;
    }
}
