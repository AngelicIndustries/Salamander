package industries.angelic.salamander.cem.client;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelLargeChest;
import net.minecraft.client.model.ModelRenderer;

import org.lwjgl.opengl.GL11;

/** Current CEM chest coordinates, with unchanged native cubes retained beneath removable fallback nodes. */
public final class CemChests {

    private static final Map<ModelChest, Chest> MODELS = new IdentityHashMap<>();

    private CemChests() {}

    public static void clear() {
        MODELS.clear();
    }

    public static boolean render(ModelChest nativeModel) {
        if (nativeModel instanceof ModelLargeChest || !industries.angelic.salamander.config.CemConfig.enabled
            || !CemResources.INSTANCE.hasModels()) return false;
        String target = CemTargets.target(CemRuntime.subject());
        if (target == null
            || !(target.equals("chest") || target.equals("trapped_chest") || target.equals("ender_chest"))
            || !CemResources.INSTANCE.hasTargets(Collections.singletonList(target))) return false;
        Chest model = MODELS.computeIfAbsent(nativeModel, Chest::new);
        model.parts.get("lid").rotateAngleX = nativeModel.chestLid.rotateAngleX;
        model.parts.get("knob").rotateAngleX = nativeModel.chestLid.rotateAngleX;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0, 1, 1);
            GL11.glRotatef(180, 1, 0, 0);
            CemRuntime.Draw previous = CemRuntime
                .beginNamed(model, null, 0, 0, 0, 0, 0, Collections.singletonList(target));
            try {
                for (ModelRenderer part : model.parts.values()) part.render(.0625f);
            } finally {
                CemRuntime.end(previous, .0625f);
            }
        } finally {
            GL11.glPopMatrix();
        }
        return true;
    }

    private static final class Chest extends ModelBase implements CemModelParts {

        final Map<String, ModelRenderer> parts = new LinkedHashMap<>();

        Chest(ModelChest original) {
            textureWidth = 64;
            textureHeight = 64;
            part("base", original.chestBelow, 0, 0, 0, 1, 10, 15);
            part("lid", original.chestLid, 0, 9, 1, 1, 0, 0);
            part("knob", original.chestKnob, 0, 9, 1, 8, 0, 0);
        }

        private void part(String name, ModelRenderer original, float x, float y, float z, float cx, float cy,
            float cz) {
            ModelRenderer part = new ModelRenderer(this), fallback = new ModelRenderer(this);
            part.setRotationPoint(x, y, z);
            fallback.setRotationPoint(cx, cy, cz);
            fallback.rotateAngleX = (float) Math.PI;
            fallback.cubeList.addAll(original.cubeList);
            part.addChild(fallback);
            parts.put(name, part);
        }

        @Override
        public Map<String, ModelRenderer> salamander$cemParts() {
            return parts;
        }
    }
}
