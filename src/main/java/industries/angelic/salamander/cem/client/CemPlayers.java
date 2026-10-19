package industries.angelic.salamander.cem.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;

import com.google.common.collect.MapMaker;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;

import industries.angelic.salamander.compat.wawelauth.WawelAuthCompat;

/** Uses native biped parts and profile metadata without depending on a particular modern-skin mod. */
public final class CemPlayers {

    // Entity.hashCode follows its network ID, which can change after a skin callback. Use weak identity keys.
    private static final Map<AbstractClientPlayer, Profile> PROFILES = new MapMaker().weakKeys()
        .makeMap();
    private static final Map<AbstractClientPlayer, Boolean> SKIN_MODELS = new MapMaker().weakKeys()
        .makeMap();
    private static final java.util.Set<ModelBase> MODELS = java.util.Collections.newSetFromMap(new WeakHashMap<>());

    private CemPlayers() {}

    public static boolean model(Object subject, ModelBase model) {
        return subject instanceof AbstractClientPlayer && model instanceof ModelBiped && MODELS.contains(model);
    }

    public static boolean registered(ModelBase model) {
        return MODELS.contains(model);
    }

    public static void register(ModelBiped model) {
        MODELS.add(model);
    }

    /** SkinManager can resolve metadata which was missing from the initial player profile. */
    public static void skinModel(AbstractClientPlayer player, boolean slim) {
        SKIN_MODELS.put(player, slim);
    }

    public static boolean slim(AbstractClientPlayer player, ModelBase model) {
        Boolean wawel = WawelAuthCompat.slim(player);
        if (wawel != null) return wawel;
        ModelBiped biped = (ModelBiped) model;
        // A modern renderer can resolve local/provider-specific skins before its model is drawn.
        if (biped.bipedRightArm.textureHeight == biped.bipedRightArm.textureWidth
            && !biped.bipedRightArm.cubeList.isEmpty()) {
            ModelBox arm = biped.bipedRightArm.cubeList.get(0);
            return Math.abs(arm.posX2 - arm.posX1 - 3) < .01;
        }
        Boolean resolved = SKIN_MODELS.get(player);
        if (resolved != null) return resolved;
        String value = "";
        for (Property property : player.getGameProfile()
            .getProperties()
            .get("textures")) {
            value = property.getValue();
            break;
        }
        Profile cached = PROFILES.get(player);
        if (cached == null || !cached.value.equals(value)) {
            cached = new Profile(value, slimProfile(value));
            PROFILES.put(player, cached);
        }
        return cached.slim;
    }

    public static boolean slimProfile(String value) {
        if (value == null || value.length() > 65536) return false;
        try {
            JsonObject textures = new JsonParser().parse(
                new String(
                    Base64.getDecoder()
                        .decode(value),
                    StandardCharsets.UTF_8))
                .getAsJsonObject()
                .getAsJsonObject("textures");
            JsonObject skin = textures.getAsJsonObject("SKIN");
            return skin.has("metadata") && skin.getAsJsonObject("metadata")
                .has("model")
                && "slim".equals(
                    skin.getAsJsonObject("metadata")
                        .get("model")
                        .getAsString());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static void clear() {
        PROFILES.clear();
    }

    private static final class Profile {

        final String value;
        final boolean slim;

        Profile(String value, boolean slim) {
            this.value = value;
            this.slim = slim;
        }
    }
}
