package industries.angelic.salamander.debug.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import industries.angelic.salamander.Salamander;

/** Cycles through the bundled GeckoLib compatibility fixtures when right-clicked. */
public final class DebugModelEntity extends EntityCreature implements GeoEntity {

    private static final int FIXTURE_WATCHER = 20;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public DebugModelEntity(World world) {
        super(world);
        setSize(0.8F, 1.6F);
        this.tasks.addTask(1, new EntityAIWander(this, 0.8));
        this.tasks.addTask(2, new EntityAIWatchClosest(this, EntityPlayer.class, 8));
        this.tasks.addTask(3, new EntityAILookIdle(this));
        func_110163_bv();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(FIXTURE_WATCHER, Byte.valueOf((byte) 0));
    }

    public Fixture getFixture() {
        return Fixture.values()[this.dataWatcher.getWatchableObjectByte(FIXTURE_WATCHER) % Fixture.values().length];
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DebugModelEntity>("fixture", 5, test -> {
            RawAnimation animation = test.isMoving() ? getFixture().movingAnimation : getFixture().idleAnimation;

            return animation == null ? PlayState.STOP : test.setAndContinue(animation);
        }).setParticleKeyframeHandler(event -> {
            DebugModelEntity entity = event.animatable();

            entity.worldObj
                .spawnParticle("happyVillager", entity.posX, entity.posY + entity.height, entity.posZ, 0, 0.05, 0);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    @Override
    public boolean interact(EntityPlayer player) {
        if (!this.worldObj.isRemote) {
            Fixture[] fixtures = Fixture.values();
            int direction = player.isSneaking() ? -1 : 1;
            int next = (getFixture().ordinal() + direction + fixtures.length) % fixtures.length;

            this.dataWatcher.updateObject(FIXTURE_WATCHER, Byte.valueOf((byte) next));
            player.addChatMessage(
                new ChatComponentText(
                    "Salamander model " + (next + 1) + "/" + fixtures.length + ": " + getFixture().displayName));
        }

        return true;
    }

    public enum Fixture {

        CREEPER("creeper", "textures/debug/creeper.png", "creeper_idle", "creeper_walk", "Creeper"),
        BAT("bat", "textures/debug/bat.png", "animation.bat.idle", "animation.bat.walk", "Bat"),
        LAYER("layer", "textures/debug/layer.png", "animation.geoLayerEntity.idle", "animation.geoLayerEntity.walk",
            "Render layers"),
        NPC("npc", "textures/debug/npc.png", "rotate_hand", "rotate_hand", "Locator NPC"),
        MAGMA_SPIDER("magma_spider", "textures/debug/magma_spider.png", "animation.magmaspider.idle",
            "animation.magmaspider.walk", "Magma spider"),
        JESTER("jester", "textures/debug/jester.png", "firework", "firework", "Jack-in-the-box jester");

        public final ResourceLocation modelResource;
        public final ResourceLocation animationResource;
        public final ResourceLocation textureResource;
        public final RawAnimation idleAnimation;
        public final RawAnimation movingAnimation;
        public final String displayName;

        Fixture(String resourceName, String texturePath, String idleAnimation, String movingAnimation,
            String displayName) {
            this.modelResource = new ResourceLocation(Salamander.MODID, "debug/" + resourceName);
            this.animationResource = new ResourceLocation(Salamander.MODID, "debug/" + resourceName);
            this.textureResource = new ResourceLocation(Salamander.MODID, texturePath);
            this.idleAnimation = loop(idleAnimation);
            this.movingAnimation = loop(movingAnimation);
            this.displayName = displayName;
        }

        private static RawAnimation loop(String animation) {
            return animation == null ? null
                : RawAnimation.begin()
                    .thenLoop(animation);
        }
    }
}
