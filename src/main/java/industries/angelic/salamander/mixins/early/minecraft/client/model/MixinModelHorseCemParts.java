package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelHorse;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelHorse.class)
public abstract class MixinModelHorseCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer head;
    @Shadow
    private ModelRenderer mouthTop;
    @Shadow
    private ModelRenderer horseLeftEar;
    @Shadow
    private ModelRenderer horseRightEar;
    @Shadow
    private ModelRenderer neck;
    @Shadow
    private ModelRenderer mane;
    @Shadow
    private ModelRenderer body;
    @Shadow
    private ModelRenderer tailBase;
    @Shadow
    private ModelRenderer backLeftLeg;
    @Shadow
    private ModelRenderer backRightLeg;
    @Shadow
    private ModelRenderer frontLeftLeg;
    @Shadow
    private ModelRenderer frontRightLeg;
    @Shadow
    private ModelRenderer muleLeftChest;
    @Shadow
    private ModelRenderer muleRightChest;
    @Shadow
    private ModelRenderer horseSaddleBottom;
    @Shadow
    private ModelRenderer horseLeftFaceMetal;
    @Shadow
    private ModelRenderer horseRightFaceMetal;
    @Shadow
    private ModelRenderer horseLeftRein;
    @Shadow
    private ModelRenderer horseRightRein;
    @Shadow
    private ModelRenderer horseFaceRopes;
    @Shadow
    private ModelRenderer mouthBottom;

    @Shadow
    private ModelRenderer backLeftShin;
    @Shadow
    private ModelRenderer backLeftHoof;
    @Shadow
    private ModelRenderer backRightShin;
    @Shadow
    private ModelRenderer backRightHoof;
    @Shadow
    private ModelRenderer frontLeftShin;
    @Shadow
    private ModelRenderer frontLeftHoof;
    @Shadow
    private ModelRenderer frontRightShin;
    @Shadow
    private ModelRenderer frontRightHoof;
    @Shadow
    private ModelRenderer tailMiddle;
    @Shadow
    private ModelRenderer tailTip;
    @Shadow
    private ModelRenderer horseSaddleFront;
    @Shadow
    private ModelRenderer horseSaddleBack;
    @Shadow
    private ModelRenderer horseLeftSaddleRope;
    @Shadow
    private ModelRenderer horseLeftSaddleMetal;
    @Shadow
    private ModelRenderer horseRightSaddleRope;
    @Shadow
    private ModelRenderer horseRightSaddleMetal;
    @Shadow
    private ModelRenderer muleLeftEar;
    @Shadow
    private ModelRenderer muleRightEar;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", head);
        parts.put("mouth", mouthTop);
        parts.put("left_ear", horseLeftEar);
        parts.put("right_ear", horseRightEar);
        parts.put("neck", neck);
        parts.put("mane", mane);
        parts.put("body", body);
        parts.put("tail", tailBase);
        parts.put("back_left_leg", backLeftLeg);
        parts.put("back_right_leg", backRightLeg);
        parts.put("front_left_leg", frontLeftLeg);
        parts.put("front_right_leg", frontRightLeg);
        parts.put("left_chest", muleLeftChest);
        parts.put("right_chest", muleRightChest);
        parts.put("saddle", horseSaddleBottom);
        parts.put("left_saddle_mouth", horseLeftFaceMetal);
        parts.put("right_saddle_mouth", horseRightFaceMetal);
        parts.put("left_saddle_line", horseLeftRein);
        parts.put("right_saddle_line", horseRightRein);
        parts.put("head_saddle", horseFaceRopes);
        parts.put("$group:mouth:bottom", mouthBottom);
        parts.put("$group:back_left_leg:Shin", backLeftShin);
        parts.put("$group:back_left_leg:Hoof", backLeftHoof);
        parts.put("$group:back_right_leg:Shin", backRightShin);
        parts.put("$group:back_right_leg:Hoof", backRightHoof);
        parts.put("$group:front_left_leg:Shin", frontLeftShin);
        parts.put("$group:front_left_leg:Hoof", frontLeftHoof);
        parts.put("$group:front_right_leg:Shin", frontRightShin);
        parts.put("$group:front_right_leg:Hoof", frontRightHoof);
        parts.put("$group:tail:tailMiddle", tailMiddle);
        parts.put("$group:tail:tailTip", tailTip);
        parts.put("$group:saddle:horseSaddleFront", horseSaddleFront);
        parts.put("$group:saddle:horseSaddleBack", horseSaddleBack);
        parts.put("$group:saddle:horseLeftSaddleRope", horseLeftSaddleRope);
        parts.put("$group:saddle:horseLeftSaddleMetal", horseLeftSaddleMetal);
        parts.put("$group:saddle:horseRightSaddleRope", horseRightSaddleRope);
        parts.put("$group:saddle:horseRightSaddleMetal", horseRightSaddleMetal);
        parts.put("$alias:left_ear", muleLeftEar);
        parts.put("$alias:right_ear", muleRightEar);
        return parts;
    }
}
