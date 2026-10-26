package industries.angelic.salamander.cem.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBat;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBoat;
import net.minecraft.client.model.ModelHorse;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelVillager;

import industries.angelic.salamander.cem.model.CemModel;

/** One native model's names, repeated parts, hierarchy and pose. Native fields remain untouched. */
public final class CemBinding {

    public final ModelBase nativeModel;
    public final int textureWidth, textureHeight;
    public final Map<String, ModelRenderer> parts = new LinkedHashMap<>();
    private final Map<String, ModelRenderer> alternateParts = new HashMap<>();
    public final Map<ModelRenderer, String> names = new IdentityHashMap<>();
    public final Map<ModelRenderer, List<String>> occurrences = new IdentityHashMap<>();
    public final Map<String, float[]> pivots = new LinkedHashMap<>();
    public final Map<String, String> parents = new LinkedHashMap<>();
    public final Map<String, String> groups = new HashMap<>();
    private final Map<String, ModelRenderer> namedParts;
    final Map<net.minecraft.util.ResourceLocation, CemResources.Entry> entries = new HashMap<>();
    List<String> candidates;
    Class<?> candidateType;
    int candidateFlags;
    String candidateTarget;
    net.minecraft.util.ResourceLocation candidateTexture;

    public CemBinding(ModelBase model, Map<String, ModelRenderer> named) {
        nativeModel = model;
        namedParts = new LinkedHashMap<>(named);
        ModelRenderer first = named.values()
            .stream()
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        textureWidth = first == null ? model.textureWidth : (int) first.textureWidth;
        textureHeight = first == null ? model.textureHeight : (int) first.textureHeight;
        for (Map.Entry<String, ModelRenderer> entry : named.entrySet()) if (entry.getValue() != null) {
            String name = entry.getKey();
            ModelRenderer part = entry.getValue();
            if (name.startsWith("$alias:")) {
                alternateParts.put(name.substring(7), part);
                names.put(part, name.substring(7));
                occurrences.put(part, Collections.singletonList(name.substring(7)));
                continue;
            }
            parts.put(name, part);
            names.putIfAbsent(part, name);
            occurrences.computeIfAbsent(part, ignored -> new ArrayList<>())
                .add(name);
            if (name.startsWith("$group:")) groups.put(name, name.split(":")[1]);
        }
        for (ModelRenderer part : model.boxList) if (!names.containsKey(part)) {
            String name = "$native" + names.size();
            parts.put(name, part);
            names.put(part, name);
            occurrences.put(part, Collections.singletonList(name));
        }
        for (Map.Entry<String, ModelRenderer> entry : parts.entrySet()) {
            String name = entry.getKey();
            ModelRenderer part = entry.getValue();
            pivots.put(name, transform(name, part));
            int occurrence = occurrences.get(part)
                .indexOf(name);
            if (part.childModels != null) for (ModelRenderer child : part.childModels) {
                List<String> childNames = occurrences.get(child);
                if (childNames != null) parents.put(childNames.get(Math.min(occurrence, childNames.size() - 1)), name);
            }
        }
        if (model instanceof ModelHorse) virtual("mouth_saddle_wrap", "root");
        if (model instanceof ModelBat) virtual("feet", "body");
        if (model instanceof ModelVillager) {
            virtual("headwear", "head");
            virtual("headwear2", "headwear");
            virtual("bodywear", "body");
        }
        if (model instanceof ModelBiped) {
            virtual("left_ear", "head");
            virtual("right_ear", "head");
            virtual("jacket", CemPlayers.registered(model) ? "root" : "body");
            virtual("left_sleeve", CemPlayers.registered(model) ? "root" : "left_arm");
            virtual("right_sleeve", CemPlayers.registered(model) ? "root" : "right_arm");
            virtual("left_pants", CemPlayers.registered(model) ? "root" : "left_leg");
            virtual("right_pants", CemPlayers.registered(model) ? "root" : "right_leg");
        }
        if (model.getClass()
            .getName()
            .endsWith(".ModelNewBoat")
            || model.getClass()
                .getName()
                .endsWith(".ModelRaft")) {
            virtual("chest_base", "root", -6, -3, -6);
            virtual("chest_lid", "root", -6, -7, -6);
            virtual("chest_knob", "root", -1, -4, -7);
        }
        if (model instanceof ModelBoat) {
            virtual("paddle_left", "root");
            virtual("paddle_right", "root");
        }
        if (model.getClass()
            .getName()
            .equals("ganymedes01.etfuturum.client.model.ModelRaft")) groups.put("back", "bottom");
    }

    /** Player renderers may swap native arm objects when alternating between wide and slim skins. */
    public boolean matches(Map<String, ModelRenderer> named) {
        return namedParts.equals(named);
    }

    private void virtual(String name, String parent, float x, float y, float z) {
        virtual(name, parent);
        pivots.put(name, new float[] { x, y, z, 0, 0, 0, 1, 1, 1 });
    }

    private void virtual(String name, String parent) {
        if (!pivots.containsKey(name)) {
            pivots.put(name, new float[] { 0, 0, 0, 0, 0, 0, 1, 1, 1 });
            parents.put(name, parent);
        }
    }

    public static boolean rightDragonPart(String name) {
        return name != null && (name.startsWith("right_") || name.contains("_right_"));
    }

    public float[] transform(String name, ModelRenderer part) {
        boolean mirror = nativeModel instanceof net.minecraft.client.model.ModelDragon && rightDragonPart(name);
        return new float[] { mirror ? -part.rotationPointX : part.rotationPointX, part.rotationPointY,
            part.rotationPointZ, part.rotateAngleX, mirror ? -part.rotateAngleY : part.rotateAngleY,
            mirror ? -part.rotateAngleZ : part.rotateAngleZ, 1, 1, 1 };
    }

    /** Modern horse models bake baby size into bones; legacy rendering scales separate body groups. */
    public boolean bakedHorseBaby(CemModel.Node node) {
        return nativeModel instanceof ModelHorse && nativeModel.isChild && !node.vanillaGeometry;
    }

    private float[] renderedTransform(CemModel.Node node, ModelRenderer part) {
        float[] value = transform(node.vanillaPart, part);
        if (bakedHorseBaby(node)) {
            String name = node.vanillaPart;
            boolean head = "neck".equals(name) || "head".equals(name)
                || "mouth".equals(name)
                || "mane".equals(name)
                || "left_ear".equals(name)
                || "right_ear".equals(name);
            float size = head ? 1.5f / 2.7272f : .5f;
            value[0] *= size;
            value[1] = (value[1] + (head ? 16.2f : 20)) * size;
            value[2] = (value[2] + (head ? 1.36f : 0)) * size;
            value[6] = value[7] = value[8] = size;
        }
        return value;
    }

    public Map<String, ModelRenderer> parts(Object subject) {
        if (!(subject instanceof net.minecraft.entity.passive.EntityHorse)) return parts;
        int type = ((net.minecraft.entity.passive.EntityHorse) subject).getHorseType();
        if (type != 1 && type != 2) return parts;
        Map<String, ModelRenderer> selected = new LinkedHashMap<>(parts);
        selected.putAll(alternateParts);
        return selected;
    }

    private boolean visible(String name, ModelRenderer part, Object subject) {
        if (!part.showModel || part.isHidden) return false;
        if (subject instanceof net.minecraft.entity.passive.EntityHorse) {
            net.minecraft.entity.passive.EntityHorse horse = (net.minecraft.entity.passive.EntityHorse) subject;
            if (name.endsWith("_chest")) return horse.isAdultHorse() && horse.isChested();
            if (name.contains("saddle")) return horse.isAdultHorse() && horse.isHorseSaddled()
                && (!name.endsWith("_line") || horse.riddenByEntity != null);
        }
        return true;
    }

    public void pose(CemModel model, double[] pose, Object subject, Map<String, ModelRenderer> selected) {
        for (Map.Entry<String, ModelRenderer> entry : selected.entrySet()) {
            CemModel.Node node = model.originalParts.get(entry.getKey());
            if (node == null) continue;
            ModelRenderer part = entry.getValue();
            float[] transform = renderedTransform(node, part);
            int offset = node.index * CemModel.STRIDE;
            for (int i = 0; i < 9; i++) pose[offset + i] = transform[i];
            pose[offset + 9] = visible(entry.getKey(), part, subject) ? 1 : 0;
        }
        if (nativeModel instanceof ModelHorse && nativeModel.isChild) {
            CemModel.Node neck = model.originalParts.get("neck");
            ModelRenderer body = selected.get("body");
            if (neck != null && body != null) {
                int y = neck.index * CemModel.STRIDE + 1;
                // Modern foals bake BabyModelTransform into their head pivot. Legacy horses keep the adult
                // pivot and scale at render time. Expose the modern pose to expressions which infer rearing
                // from neck.ty; unreplaced geometry still uses the legacy pivot and render-time scaling.
                double rear = -body.rotateAngleX / (Math.PI / 4);
                double babyRestY = (4 + 16.2f) * (1.5f / 2.7272f);
                pose[y] = babyRestY + (selected.get("neck").rotationPointY - 4 + 10 * rear) * .5 - 4 * rear;
            }
        }
        boolean chestBoat = "chest_boat".equals(CemTargets.target(subject)) && !model.source.getResourcePath()
            .matches(".*_patch[0-9]*\\.jem");
        for (String name : new String[] { "chest_base", "chest_lid", "chest_knob" }) {
            CemModel.Node node = model.originalParts.get(name);
            if (node != null && !selected.containsKey(name)) pose[node.index * CemModel.STRIDE + 9] = chestBoat ? 1 : 0;
        }
    }

    /** Player overlays are independent CEM slots. Older packs animate them explicitly; newer ones inherit. */
    public void playerLayers(CemModel.Instance instance, boolean staticPose) {
        if (!CemPlayers.registered(nativeModel)) return;
        for (String[] pair : new String[][] { { "head", "headwear" }, { "body", "jacket" },
            { "right_arm", "right_sleeve" }, { "left_arm", "left_sleeve" }, { "right_leg", "right_pants" },
            { "left_leg", "left_pants" } }) instance.followPose(pair[0], pair[1], staticPose);
    }

    /** Old models such as dragons and crystals reuse the same field for multiple named CEM parts. */
    public void refresh(CemModel.Node node, CemModel.Instance instance, Object subject,
        Map<String, ModelRenderer> selected) {
        ModelRenderer part = selected.get(node.vanillaPart);
        if (part != null)
            instance.nativePose(node, renderedTransform(node, part), visible(node.vanillaPart, part, subject));
        // Biped headwear is a separate native render call. Its vanilla copy happens before CEM evaluation,
        // so it must follow the evaluated head here (packs can put the whole visible head in this slot).
        if (nativeModel instanceof ModelBiped && "headwear".equals(node.vanillaPart))
            instance.followPose("head", "headwear");
        for (CemModel.Node child : node.children) refresh(child, instance, subject, selected);
    }
}
