package industries.angelic.salamander.cem.client;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import industries.angelic.salamander.cem.model.CemModel;

/** Frame clock and per-entity animation ownership independent of the rendering backend. */
public final class CemClient {

    public static final CemClient INSTANCE = new CemClient();
    private static final Map<Object, State> STATES = new WeakHashMap<>();
    private static World world;
    private static long frame;
    private static long previousTime;
    private static double frameTime;

    private CemClient() {}

    @SubscribeEvent
    public void beginFrame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        World current = Minecraft.getMinecraft().theWorld;
        if (world != current) {
            clearAnimationState();
            world = current;
        }
        long time = System.nanoTime();
        frameTime = previousTime == 0 ? 0 : Math.min(1, Math.max(0, (time - previousTime) / 1_000_000_000d));
        previousTime = time;
        frame++;
        CemBeds.frame();
    }

    public static void clearAnimationState() {
        STATES.clear();
        CemPlayers.clear();
        CemPlayerSkins.clear();
        CemRuleFacts.clear();
        CemRandomTextures.clearEntities();
        previousTime = 0;
    }

    public static CemModel.Instance instance(Object entity, CemModel model) {
        State state = STATES.computeIfAbsent(entity, ignored -> new State());
        return state.instances.computeIfAbsent(model, ignored -> model.newInstance(state.variables));
    }

    public static long frame() {
        return frame;
    }

    public static Map<String, Double> inputs(Object subject, float partialTicks, float limbSwing, float limbSpeed,
        float age, float headYaw, float headPitch) {
        Map<String, Double> values = new HashMap<>();
        net.minecraft.entity.Entity entity = subject instanceof net.minecraft.entity.Entity
            ? (net.minecraft.entity.Entity) subject
            : null;
        EntityLivingBase living = entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
        values.put("frame_counter", (double) (frame % 27720));
        values.put("frame_time", frameTime);
        values.put("age", entity == null ? (double) age : age(entity, partialTicks));
        values.put("limb_swing", (double) limbSwing);
        values.put("limb_speed", (double) limbSpeed);
        values.put("head_yaw", (double) MathHelper.wrapAngleTo180_float(headYaw));
        values.put("head_pitch", (double) headPitch);
        EntityLivingBase player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            values.put("player_pos_x", interpolate(player.prevPosX, player.posX, partialTicks));
            values.put("player_pos_y", interpolate(player.prevPosY, player.posY, partialTicks));
            values.put("player_pos_z", interpolate(player.prevPosZ, player.posZ, partialTicks));
            values.put(
                "player_rot_x",
                Math.toRadians(interpolateAngle(player.prevRotationPitch, player.rotationPitch, partialTicks)));
            values.put(
                "player_rot_y",
                Math.toRadians(interpolateAngle(player.prevRotationYaw, player.rotationYaw, partialTicks)));
        }
        values.put("is_in_hand", CemItemContext.hand() ? 1d : 0d);
        values.put("is_in_gui", CemItemContext.gui() ? 1d : 0d);
        values.put("is_in_item_frame", CemRuntime.itemFrame() ? 1d : 0d);
        if (entity == null) {
            if (subject instanceof net.minecraft.tileentity.TileEntity) {
                net.minecraft.tileentity.TileEntity tile = (net.minecraft.tileentity.TileEntity) subject;
                values.put("pos_x", (double) tile.xCoord);
                values.put("pos_y", (double) tile.yCoord);
                values.put("pos_z", (double) tile.zCoord);
                if (tile.getWorldObj() != null) {
                    values.put(
                        "time",
                        (tile.getWorldObj()
                            .getTotalWorldTime() % 27720) + (double) partialTicks);
                    values.put("dimension", (double) tile.getWorldObj().provider.dimensionId);
                    values.put(
                        "day_time",
                        (tile.getWorldObj()
                            .getWorldTime() % 24000) + (double) partialTicks);
                    values.put(
                        "day_count",
                        (double) (tile.getWorldObj()
                            .getWorldTime() / 24000));
                }
                values.put("id", (double) (tile.xCoord * 73428767 ^ tile.yCoord * 912931 ^ tile.zCoord * 438289));
            } else if (!CemItemContext.hand() && !CemRuntime.itemFrame()) values.put("is_in_gui", 1d);
            return values;
        }
        values.put(
            "rot_x",
            Math.toRadians(interpolateAngle(entity.prevRotationPitch, entity.rotationPitch, partialTicks)));
        values.put(
            "rot_y",
            Math.toRadians(
                interpolateAngle(
                    living == null ? entity.prevRotationYaw : living.prevRenderYawOffset,
                    living == null ? entity.rotationYaw : living.renderYawOffset,
                    partialTicks)));
        values.put("pos_x", interpolate(entity.prevPosX, entity.posX, partialTicks));
        values.put("pos_y", interpolate(entity.prevPosY, entity.posY, partialTicks));
        values.put("pos_z", interpolate(entity.prevPosZ, entity.posZ, partialTicks));
        if (player != null) {
            double dx = values.get("pos_x") - values.get("player_pos_x");
            double dy = values.get("pos_y") - values.get("player_pos_y");
            double dz = values.get("pos_z") - values.get("player_pos_z");
            values.put("distance", Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
        values.put("id", (double) entity.getEntityId());
        if (living != null) {
            double[] movement = industries.angelic.salamander.cem.animation.CemMovement.direction(
                entity.posX - entity.prevPosX,
                entity.posZ - entity.prevPosZ,
                interpolateAngle(living.prevRenderYawOffset, living.renderYawOffset, partialTicks));
            values.put("move_forward", movement[0]);
            values.put("move_strafing", movement[1]);
            values.put("is_climbing", living.isOnLadder() ? 1d : 0d);
            values.put("is_gliding", CemBackportPoses.gliding(living) ? 1d : 0d);
            values.put("health", (double) living.getHealth());
            values.put("max_health", (double) living.getMaxHealth());
            values.put("hurt_time", living.hurtTime > 0 ? Math.max(0, living.hurtTime - partialTicks) : 0d);
            values.put("death_time", living.deathTime > 0 ? living.deathTime + (double) partialTicks : 0d);
            values.put("swing_progress", CemClientSignals.swing(living, partialTicks));
            values.put("is_aggressive", CemClientSignals.aggressive(living) ? 1d : 0d);
            values.put("is_hurt", living.hurtTime > 0 ? 1d : 0d);
            values.put("is_child", living.isChild() ? 1d : 0d);
        }
        if (entity instanceof net.minecraft.entity.passive.EntityTameable) {
            net.minecraft.entity.passive.EntityTameable tame = (net.minecraft.entity.passive.EntityTameable) entity;
            values.put("is_tamed", tame.isTamed() ? 1d : 0d);
            values.put("is_sitting", tame.isSitting() ? 1d : 0d);
        }
        if (entity instanceof net.minecraft.entity.passive.EntityHorse)
            values.put("is_tamed", ((net.minecraft.entity.passive.EntityHorse) entity).isTame() ? 1d : 0d);
        values.put("is_alive", entity.isEntityAlive() ? 1d : 0d);
        values.put("is_on_ground", entity.onGround ? 1d : 0d);
        values.put("is_ridden", entity.riddenByEntity != null ? 1d : 0d);
        values.put("is_riding", entity.isRiding() ? 1d : 0d);
        values.put("is_burning", entity.isBurning() ? 1d : 0d);
        values.put("is_in_water", entity.isInWater() ? 1d : 0d);
        values.put("is_in_lava", entity.handleLavaMovement() ? 1d : 0d);
        values.put("is_wet", entity.isWet() ? 1d : 0d);
        values.put("is_sneaking", entity.isSneaking() ? 1d : 0d);
        values.put("is_sprinting", entity.isSprinting() ? 1d : 0d);
        values.put("is_invisible", entity.isInvisible() ? 1d : 0d);
        values.put("dimension", (double) entity.dimension);
        values.put("time", (entity.worldObj.getTotalWorldTime() % 27720) + (double) partialTicks);
        values.put("day_time", (entity.worldObj.getWorldTime() % 24000) + (double) partialTicks);
        values.put("day_count", (double) (entity.worldObj.getWorldTime() / 24000));
        if (subject instanceof CemEntityInputs) ((CemEntityInputs) subject).salamander$cemInputs(values, partialTicks);
        return values;
    }

    /**
     * Entities outside the world, such as a quest book's player portrait, never tick. Their age follows the client
     * clock instead, so animations keep playing rather than freezing on one frame, mid-blink for example.
     */
    private static double age(net.minecraft.entity.Entity entity, float partialTicks) {
        World world = entity.worldObj;
        if (world != null && world.getEntityByID(entity.getEntityId()) == entity)
            return entity.ticksExisted + (double) partialTicks;
        return entity.ticksExisted + Minecraft.getSystemTime() / 50.0;
    }

    private static double interpolate(double previous, double current, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }

    private static float interpolateAngle(float previous, float current, float partialTicks) {
        return previous + MathHelper.wrapAngleTo180_float(current - previous) * partialTicks;
    }

    private static final class State {

        final Map<String, Double> variables = new HashMap<>();
        final Map<CemModel, CemModel.Instance> instances = new java.util.IdentityHashMap<>();
    }
}
