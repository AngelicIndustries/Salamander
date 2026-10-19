package industries.angelic.salamander.mixins.late.etfuturum;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.salamander.cem.client.CemModelParts;
import industries.angelic.salamander.cem.client.CemRuntime;
import industries.angelic.salamander.cem.client.CemTargets;

@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.client.model.ModelNewBoat", remap = false)
public abstract class MixinModelNewBoatCem implements CemModelParts {

    @Shadow
    @Final
    private ModelRenderer[] boatSides;
    @Shadow
    @Final
    private ModelRenderer[] paddles;
    @Shadow
    private ModelRenderer noWater;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("bottom", boatSides.length > 0 ? boatSides[0] : null);
        parts.put("back", boatSides.length > 1 ? boatSides[1] : null);
        parts.put("front", boatSides.length > 2 ? boatSides[2] : null);
        parts.put("right", boatSides.length > 3 ? boatSides[3] : null);
        parts.put("left", boatSides.length > 4 ? boatSides[4] : null);
        parts.put("paddle_left", paddles.length > 0 ? paddles[0] : null);
        parts.put("paddle_right", paddles.length > 1 ? paddles[1] : null);
        parts.put("water_patch", noWater);
        return parts;
    }

    @WrapMethod(
        method = { "render(Lnet/minecraft/entity/Entity;FFFFFF)V",
            "func_78088_a(Lnet/minecraft/entity/Entity;FFFFFF)V" },
        remap = false)
    private void salamander$render(Entity entity, float limb, float speed, float age, float yaw, float pitch,
        float scale, Operation<Void> original) {
        CemRuntime.Draw previous = CemRuntime.begin((ModelBase) (Object) this, entity, limb, speed, age, yaw, pitch);
        try {
            original.call(entity, limb, speed, age, yaw, pitch, scale);
        } finally {
            CemRuntime.end(previous, scale);
        }
    }

    @WrapMethod(method = "renderMultipass", remap = false)
    private void salamander$patch(Entity entity, float limb, float speed, float age, float yaw, float pitch,
        float scale, Operation<Void> original) {
        CemRuntime.Draw previous = CemRuntime.beginNamed(
            (ModelBase) (Object) this,
            entity,
            limb,
            speed,
            age,
            yaw,
            pitch,
            CemTargets.patchCandidates(entity, (ModelBase) (Object) this, CemRuntime.texture()));
        try {
            original.call(entity, limb, speed, age, yaw, pitch, scale);
        } finally {
            CemRuntime.end(previous, scale);
        }
    }
}
