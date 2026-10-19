/*
 * Derived from Alex's Mobs, Copyright (c) AlexModGuy and contributors.
 * SPDX-License-Identifier: LGPL-3.0-only
 * Modified as a Salamander rendering fixture.
 */
package industries.angelic.salamander.debug.client;

import java.util.Arrays;
import java.util.Collections;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;

import industries.angelic.salamander.debug.entity.DebugCitadelFly;

/** Alex's Mobs fly structure adapted for the opt-in Citadel fixture. */
public final class DebugCitadelFlyModel extends AdvancedEntityModel<DebugCitadelFly> {

    private final AdvancedModelBox root;
    private final AdvancedModelBox body;
    private final AdvancedModelBox legs;
    private final AdvancedModelBox leftWing;
    private final AdvancedModelBox rightWing;
    private final AdvancedModelBox mouth;
    private final ModelAnimator animator = ModelAnimator.create();

    public DebugCitadelFlyModel() {
        this.texWidth = 32;
        this.texHeight = 32;
        this.root = new AdvancedModelBox(this, "root");
        this.root.setPos(0, 24, 0);
        this.body = new AdvancedModelBox(this, "body");
        this.body.setPos(0, -3, 0);
        this.root.addChild(this.body);
        this.body.setTextureOffset(0, 0)
            .addBox(-2, -2, -3, 4F, 4F, 6F, 0, false);
        this.legs = new AdvancedModelBox(this, "legs");
        this.legs.setPos(0, 2, -2);
        this.body.addChild(this.legs);
        this.legs.setTextureOffset(0, 11)
            .addBox(-1.5F, 0, 0, 3F, 1F, 5F, 0, false);
        this.leftWing = new AdvancedModelBox(this, "left_wing");
        this.leftWing.setPos(1, -2, -1);
        this.body.addChild(this.leftWing);
        this.leftWing.setTextureOffset(12, 11)
            .addBox(0, 0, -1, 4F, 0F, 3F, 0, false);
        this.rightWing = new AdvancedModelBox(this, "right_wing");
        this.rightWing.setPos(-1, -2, -1);
        this.body.addChild(this.rightWing);
        this.rightWing.setTextureOffset(12, 11)
            .addBox(-4, 0, -1, 4F, 0F, 3F, 0, true);
        this.mouth = new AdvancedModelBox(this, "mouth");
        this.mouth.setPos(0, 0, -3);
        this.body.addChild(this.mouth);
        this.mouth.setTextureOffset(15, 16)
            .addBox(0, 0, -1, 0F, 4F, 2F, 0, false);
        updateDefaultPose();
    }

    @Override
    public void setupAnim(DebugCitadelFly fly, float limbSwing, float limbSwingAmount, float ageInTicks,
        float netHeadYaw, float headPitch) {
        resetToDefaultPose();
        this.animator.update(fly);

        if (this.animator.setAnimation(DebugCitadelFly.WING_WAVE)) {
            this.animator.startKeyframe(5);
            this.animator.rotate(this.leftWing, 0, 0, -1.4F);
            this.animator.rotate(this.rightWing, 0, 0, 1.4F);
            this.animator.endKeyframe();
            this.animator.setStaticKeyframe(5);
            this.animator.resetKeyframe(5);
        }

        walk(this.mouth, 0.28F, 0.08F, false, -1, 0.2F, ageInTicks, 1);
        flap(this.leftWing, 1.82F, 0.8F, true, 0, 0.2F, ageInTicks, 1);
        flap(this.rightWing, 1.82F, 0.8F, false, 0, 0.2F, ageInTicks, 1);
        walk(this.legs, 0.28F, 0.16F, false, 1, 0.2F, ageInTicks, 1);
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return Collections.<BasicModelPart>singletonList(this.root);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return Arrays.asList(this.root, this.body, this.leftWing, this.rightWing, this.legs, this.mouth);
    }
}
