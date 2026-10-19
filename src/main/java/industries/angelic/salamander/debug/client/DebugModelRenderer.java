package industries.angelic.salamander.debug.client;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.debug.entity.DebugModelEntity;

public final class DebugModelRenderer extends GeoEntityRenderer<DebugModelEntity> {

    public DebugModelRenderer() {
        super(new DebugModel(), 0.5F);
        addRenderLayer(new FixtureLayer(this));
    }

    private static final class FixtureLayer extends GeoRenderLayer<DebugModelEntity> {

        private static final ResourceLocation GLASSES = new ResourceLocation(
            Salamander.MODID,
            "textures/debug/layer_glasses.png");
        private static final ItemStack STICK = new ItemStack(Items.stick);

        private FixtureLayer(DebugModelRenderer renderer) {
            super(renderer);
        }

        @Override
        public void preRender(RenderPassInfo<DebugModelEntity> renderPassInfo) {
            if (renderPassInfo.animatable()
                .getFixture() == DebugModelEntity.Fixture.NPC) {
                renderPassInfo.pose()
                    .get("held_item")
                    .ifPresent(snapshot -> snapshot.skipRender(true));
            }
        }

        @Override
        public void render(RenderPassInfo<DebugModelEntity> renderPassInfo) {
            switch (renderPassInfo.animatable()
                .getFixture()) {
                case LAYER:
                    renderPassInfo.reRender(GLASSES);
                    break;
                case NPC:
                    renderPassInfo.runAtLocator("held_item", () -> {
                        GL11.glRotatef(180, 0, 0, 1);
                        GL11.glScalef(0.75F, 0.75F, 0.75F);
                        RenderManager.instance.itemRenderer.renderItem(renderPassInfo.animatable(), STICK, 0);
                    });
                    break;
                default:
                    break;
            }
        }
    }
}
