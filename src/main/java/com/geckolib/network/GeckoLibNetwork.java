package com.geckolib.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animation.RawAnimation;
import com.geckolib.cache.SyncedSingletonAnimatableCache;
import com.geckolib.network.packet.blockentity.BlockEntityAnimTriggerPacket;
import com.geckolib.network.packet.blockentity.StatelessBlockEntityPlayAnimPacket;
import com.geckolib.network.packet.blockentity.StatelessBlockEntityStopAnimPacket;
import com.geckolib.network.packet.blockentity.StopTriggeredBlockEntityAnimPacket;
import com.geckolib.network.packet.entity.CitadelAnimationPacket;
import com.geckolib.network.packet.entity.EntityAnimTriggerPacket;
import com.geckolib.network.packet.entity.StatelessEntityPlayAnimPacket;
import com.geckolib.network.packet.entity.StatelessEntityStopAnimPacket;
import com.geckolib.network.packet.entity.StopTriggeredEntityAnimPacket;
import com.geckolib.network.packet.singleton.SingletonAnimTriggerPacket;
import com.geckolib.network.packet.singleton.StatelessSingletonPlayAnimPacket;
import com.geckolib.network.packet.singleton.StatelessSingletonStopAnimPacket;
import com.geckolib.network.packet.singleton.StopTriggeredSingletonAnimPacket;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import industries.angelic.salamander.Salamander;

/** Transient server-to-client animation synchronization. */
public final class GeckoLibNetwork {

    public static final int PROTOCOL_VERSION = 1;

    private static final int ENTITY_TRIGGER_PACKET_ID = 0;
    private static final int STOP_ENTITY_TRIGGER_PACKET_ID = 1;
    private static final int SINGLETON_TRIGGER_PACKET_ID = 2;
    private static final int STOP_SINGLETON_TRIGGER_PACKET_ID = 3;
    private static final int BLOCK_ENTITY_TRIGGER_PACKET_ID = 4;
    private static final int STOP_BLOCK_ENTITY_TRIGGER_PACKET_ID = 5;
    private static final int STATELESS_ENTITY_PLAY_PACKET_ID = 6;
    private static final int STATELESS_ENTITY_STOP_PACKET_ID = 7;
    private static final int STATELESS_SINGLETON_PLAY_PACKET_ID = 8;
    private static final int STATELESS_SINGLETON_STOP_PACKET_ID = 9;
    private static final int STATELESS_BLOCK_ENTITY_PLAY_PACKET_ID = 10;
    private static final int STATELESS_BLOCK_ENTITY_STOP_PACKET_ID = 11;
    private static final int CITADEL_ANIMATION_PACKET_ID = 12;
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Salamander.MODID);

    private static boolean initialized;

    private GeckoLibNetwork() {}

    public static void init() {
        if (initialized) throw new IllegalStateException("Salamander network is already initialized");

        initialized = true;
        CHANNEL.registerMessage(
            EntityAnimTriggerPacket.Handler.class,
            EntityAnimTriggerPacket.class,
            ENTITY_TRIGGER_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            StopTriggeredEntityAnimPacket.Handler.class,
            StopTriggeredEntityAnimPacket.class,
            STOP_ENTITY_TRIGGER_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            SingletonAnimTriggerPacket.Handler.class,
            SingletonAnimTriggerPacket.class,
            SINGLETON_TRIGGER_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            StopTriggeredSingletonAnimPacket.Handler.class,
            StopTriggeredSingletonAnimPacket.class,
            STOP_SINGLETON_TRIGGER_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            BlockEntityAnimTriggerPacket.Handler.class,
            BlockEntityAnimTriggerPacket.class,
            BLOCK_ENTITY_TRIGGER_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            StopTriggeredBlockEntityAnimPacket.Handler.class,
            StopTriggeredBlockEntityAnimPacket.class,
            STOP_BLOCK_ENTITY_TRIGGER_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            StatelessEntityPlayAnimPacket.Handler.class,
            StatelessEntityPlayAnimPacket.class,
            STATELESS_ENTITY_PLAY_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            StatelessEntityStopAnimPacket.Handler.class,
            StatelessEntityStopAnimPacket.class,
            STATELESS_ENTITY_STOP_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            StatelessSingletonPlayAnimPacket.Handler.class,
            StatelessSingletonPlayAnimPacket.class,
            STATELESS_SINGLETON_PLAY_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            StatelessSingletonStopAnimPacket.Handler.class,
            StatelessSingletonStopAnimPacket.class,
            STATELESS_SINGLETON_STOP_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            StatelessBlockEntityPlayAnimPacket.Handler.class,
            StatelessBlockEntityPlayAnimPacket.class,
            STATELESS_BLOCK_ENTITY_PLAY_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            StatelessBlockEntityStopAnimPacket.Handler.class,
            StatelessBlockEntityStopAnimPacket.class,
            STATELESS_BLOCK_ENTITY_STOP_PACKET_ID,
            Side.CLIENT);
        CHANNEL.registerMessage(
            CitadelAnimationPacket.Handler.class,
            CitadelAnimationPacket.class,
            CITADEL_ANIMATION_PACKET_ID,
            Side.CLIENT);
    }

    public static void triggerEntityAnimation(Entity entity, String controllerName, String animationName) {
        EntityAnimTriggerPacket message = new EntityAnimTriggerPacket(
            entity.getEntityId(),
            controllerName,
            animationName);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid entity animation trigger");

        sendToTrackingAndSelf(entity, message);
    }

    public static void stopTriggeredEntityAnimation(Entity entity, String controllerName, String animationName) {
        StopTriggeredEntityAnimPacket message = new StopTriggeredEntityAnimPacket(
            entity.getEntityId(),
            controllerName,
            animationName);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid entity animation stop request");

        sendToTrackingAndSelf(entity, message);
    }

    public static void triggerReplacedEntityAnimation(GeoReplacedEntity animatable, Entity relatedEntity,
        String controllerName, String animationName) {
        EntityAnimTriggerPacket message = new EntityAnimTriggerPacket(
            relatedEntity.getEntityId(),
            true,
            controllerName,
            animationName);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid replaced-entity animation trigger");

        sendToTrackingAndSelf(relatedEntity, message);
    }

    public static void stopTriggeredReplacedEntityAnimation(GeoReplacedEntity animatable, Entity relatedEntity,
        String controllerName, String animationName) {
        StopTriggeredEntityAnimPacket message = new StopTriggeredEntityAnimPacket(
            relatedEntity.getEntityId(),
            true,
            controllerName,
            animationName);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid replaced-entity animation stop request");

        sendToTrackingAndSelf(relatedEntity, message);
    }

    public static void triggerSingletonAnimation(SingletonGeoAnimatable animatable, Entity relatedEntity,
        long instanceId, String controllerName, String animationName) {
        SingletonAnimTriggerPacket message = new SingletonAnimTriggerPacket(
            SyncedSingletonAnimatableCache.getOrCreateId(animatable),
            instanceId,
            controllerName,
            animationName);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid singleton animation trigger");

        sendToTrackingAndSelf(relatedEntity, message);
    }

    public static void stopTriggeredSingletonAnimation(SingletonGeoAnimatable animatable, Entity relatedEntity,
        long instanceId, String controllerName, String animationName) {
        StopTriggeredSingletonAnimPacket message = new StopTriggeredSingletonAnimPacket(
            SyncedSingletonAnimatableCache.getOrCreateId(animatable),
            instanceId,
            controllerName,
            animationName);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid singleton animation stop request");

        sendToTrackingAndSelf(relatedEntity, message);
    }

    public static void triggerBlockEntityAnimation(TileEntity tileEntity, String controllerName, String animationName) {
        BlockEntityAnimTriggerPacket message = new BlockEntityAnimTriggerPacket(
            tileEntity.xCoord,
            tileEntity.yCoord,
            tileEntity.zCoord,
            controllerName,
            animationName);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid block entity animation trigger");

        sendToTrackingChunk(tileEntity, message);
    }

    public static void stopTriggeredBlockEntityAnimation(TileEntity tileEntity, String controllerName,
        String animationName) {
        StopTriggeredBlockEntityAnimPacket message = new StopTriggeredBlockEntityAnimPacket(
            tileEntity.xCoord,
            tileEntity.yCoord,
            tileEntity.zCoord,
            controllerName,
            animationName);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid block entity animation stop request");

        sendToTrackingChunk(tileEntity, message);
    }

    public static void playStatelessEntityAnimation(Entity entity, boolean replacedEntity, RawAnimation animation) {
        playStatelessEntityAnimation(entity, entity.getEntityId(), replacedEntity, animation);
    }

    public static void playStatelessEntityAnimation(Entity trackingEntity, int entityId, boolean replacedEntity,
        RawAnimation animation) {
        StatelessEntityPlayAnimPacket message = new StatelessEntityPlayAnimPacket(entityId, replacedEntity, animation);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid stateless entity animation");

        sendToTrackingAndSelf(trackingEntity, message);
    }

    public static void stopStatelessEntityAnimation(Entity entity, boolean replacedEntity, String animation) {
        stopStatelessEntityAnimation(entity, entity.getEntityId(), replacedEntity, animation);
    }

    public static void stopStatelessEntityAnimation(Entity trackingEntity, int entityId, boolean replacedEntity,
        String animation) {
        StatelessEntityStopAnimPacket message = new StatelessEntityStopAnimPacket(entityId, replacedEntity, animation);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid stateless entity animation stop request");

        sendToTrackingAndSelf(trackingEntity, message);
    }

    public static void playStatelessSingletonAnimation(SingletonGeoAnimatable animatable, Entity relatedEntity,
        long instanceId, RawAnimation animation) {
        StatelessSingletonPlayAnimPacket message = new StatelessSingletonPlayAnimPacket(
            SyncedSingletonAnimatableCache.getOrCreateId(animatable),
            instanceId,
            animation);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid stateless singleton animation");

        sendToTrackingAndSelf(relatedEntity, message);
    }

    public static void stopStatelessSingletonAnimation(SingletonGeoAnimatable animatable, Entity relatedEntity,
        long instanceId, String animation) {
        StatelessSingletonStopAnimPacket message = new StatelessSingletonStopAnimPacket(
            SyncedSingletonAnimatableCache.getOrCreateId(animatable),
            instanceId,
            animation);

        if (!message.isValid())
            throw new IllegalArgumentException("Invalid stateless singleton animation stop request");

        sendToTrackingAndSelf(relatedEntity, message);
    }

    public static void playStatelessBlockEntityAnimation(TileEntity tileEntity, RawAnimation animation) {
        StatelessBlockEntityPlayAnimPacket message = new StatelessBlockEntityPlayAnimPacket(
            tileEntity.xCoord,
            tileEntity.yCoord,
            tileEntity.zCoord,
            animation);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid stateless block entity animation");

        sendToTrackingChunk(tileEntity, message);
    }

    public static void stopStatelessBlockEntityAnimation(TileEntity tileEntity, String animation) {
        StatelessBlockEntityStopAnimPacket message = new StatelessBlockEntityStopAnimPacket(
            tileEntity.xCoord,
            tileEntity.yCoord,
            tileEntity.zCoord,
            animation);

        if (!message.isValid())
            throw new IllegalArgumentException("Invalid stateless block entity animation stop request");

        sendToTrackingChunk(tileEntity, message);
    }

    public static void syncCitadelAnimation(Entity entity, int animationIndex) {
        CitadelAnimationPacket message = new CitadelAnimationPacket(entity.getEntityId(), animationIndex);

        if (!message.isValid()) throw new IllegalArgumentException("Invalid Citadel entity animation");

        sendToTrackingAndSelf(entity, message);
    }

    private static void sendToTrackingAndSelf(Entity entity, IMessage message) {
        if (!(entity.worldObj instanceof WorldServer)) {
            throw new IllegalArgumentException("Animation packets can only be sent for server entities");
        }

        Packet packet = CHANNEL.getPacketFrom(message);

        ((WorldServer) entity.worldObj).getEntityTracker()
            .func_151248_b(entity, packet);
    }

    private static void sendToTrackingChunk(TileEntity tileEntity, IMessage message) {
        if (!(tileEntity.getWorldObj() instanceof WorldServer))
            throw new IllegalArgumentException("Animation packets can only be sent for server block entities");

        WorldServer world = (WorldServer) tileEntity.getWorldObj();
        int chunkX = tileEntity.xCoord >> 4;
        int chunkZ = tileEntity.zCoord >> 4;

        for (Object player : world.playerEntities) {
            if (player instanceof EntityPlayerMP && world.getPlayerManager()
                .isPlayerWatchingChunk((EntityPlayerMP) player, chunkX, chunkZ))
                CHANNEL.sendTo(message, (EntityPlayerMP) player);
        }
    }
}
