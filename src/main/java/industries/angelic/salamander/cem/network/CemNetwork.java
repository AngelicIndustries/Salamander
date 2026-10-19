package industries.angelic.salamander.cem.network;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import industries.angelic.salamander.Salamander;
import industries.angelic.salamander.config.CemConfig;
import io.netty.buffer.ByteBuf;

/** Optional channel with explicit version negotiation; all entity access is on the logical server thread. */
public final class CemNetwork {

    public static final int VERSION = 1;
    public static final String NAME = "salamander_cem";
    public static final CemNetwork INSTANCE = new CemNetwork();
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(NAME);
    private final Set<NetworkManager> advertised = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ConcurrentLinkedQueue<Runnable> pending = new ConcurrentLinkedQueue<>();
    private final Map<EntityPlayerMP, Boolean> peers = new IdentityHashMap<>();
    private final Map<EntityPlayerMP, Map<EntityLiving, Tracking>> tracking = new IdentityHashMap<>();
    private final Map<EntityLiving, Integer> attacks = new WeakHashMap<>();
    private long nextToken;

    private CemNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(ServerHello.class, Hello.class, 0, Side.SERVER);
        CHANNEL.registerMessage(ClientHello.class, Hello.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(ClientSignal.class, CemSignalPacket.class, 2, Side.CLIENT);
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public static void request() {
        CHANNEL.sendToServer(new Hello(true));
    }

    public static boolean supported(Entity entity) {
        return entity instanceof EntityLiving;
    }

    /** Called immediately before an actual melee attack; never modifies the entity. */
    public static void attacked(Entity entity) {
        if (entity.worldObj.isRemote || !supported(entity) || !CemConfig.serverAnimationSignals) return;
        EntityLiving creature = (EntityLiving) entity;
        int previous = INSTANCE.attacks.getOrDefault(creature, 0);
        INSTANCE.attacks.put(creature, previous + 1);
    }

    @SubscribeEvent
    public void otherMelee(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        Entity attacker = event.source.getEntity();
        if (attacker instanceof EntityLiving && !(attacker instanceof net.minecraft.entity.monster.EntityMob)
            && !(event.source instanceof net.minecraft.util.EntityDamageSourceIndirect)) attacked(attacker);
    }

    @SubscribeEvent
    public void registrations(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if (!event.registrations.contains(NAME)) return;
        boolean registered = event.operation.equals("REGISTER");
        if (event.side == Side.SERVER) {
            if (registered) advertised.add(event.manager);
            else advertised.remove(event.manager);
        } else Salamander.proxy.handleCemRegistration(event.manager, registered);
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ServerDisconnectionFromClientEvent event) {
        advertised.remove(event.manager);
        pending.add(() -> {
            peers.keySet()
                .removeIf(player -> player.playerNetServerHandler.netManager == event.manager);
            tracking.keySet()
                .removeIf(player -> player.playerNetServerHandler.netManager == event.manager);
        });
    }

    @SubscribeEvent
    public void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.entityPlayer instanceof EntityPlayerMP) || !supported(event.target)) return;
        tracking.computeIfAbsent((EntityPlayerMP) event.entityPlayer, ignored -> new IdentityHashMap<>())
            .put((EntityLiving) event.target, new Tracking(++nextToken));
    }

    @SubscribeEvent
    public void stopTracking(PlayerEvent.StopTracking event) {
        if (!(event.entityPlayer instanceof EntityPlayerMP) || !supported(event.target)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
        Map<EntityLiving, Tracking> entities = tracking.get(player);
        if (entities == null) return;
        Tracking state = entities.remove(event.target);
        if (state != null && state.sent
            && Boolean.TRUE.equals(peers.get(player))
            && advertised.contains(player.playerNetServerHandler.netManager))
            CHANNEL.sendTo(packet((EntityLiving) event.target, state, CemSignalPacket.REMOVE), player);
        if (entities.isEmpty()) tracking.remove(player);
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) return;
        tracking.values()
            .forEach(
                entities -> entities.keySet()
                    .removeIf(entity -> entity.worldObj == event.world));
        attacks.keySet()
            .removeIf(entity -> entity.worldObj == event.world);
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Runnable task;
        while ((task = pending.poll()) != null) task.run();
        Iterator<Map.Entry<EntityPlayerMP, Boolean>> players = peers.entrySet()
            .iterator();
        while (players.hasNext()) {
            Map.Entry<EntityPlayerMP, Boolean> peer = players.next();
            EntityPlayerMP player = peer.getKey();
            if (!advertised.contains(player.playerNetServerHandler.netManager)
                || !player.playerNetServerHandler.netManager.isChannelOpen()) {
                players.remove();
                tracking.remove(player);
                continue;
            }
            boolean enabled = CemConfig.serverAnimationSignals;
            Map<EntityLiving, Tracking> entities = tracking.get(player);
            if (peer.getValue() != enabled) {
                peer.setValue(enabled);
                CHANNEL.sendTo(new Hello(enabled), player);
                if (entities != null) entities.values()
                    .forEach(state -> state.sent = false);
            }
            if (!enabled || entities == null) continue;
            Iterator<Map.Entry<EntityLiving, Tracking>> iterator = entities.entrySet()
                .iterator();
            while (iterator.hasNext()) {
                Map.Entry<EntityLiving, Tracking> tracked = iterator.next();
                EntityLiving creature = tracked.getKey();
                Tracking state = tracked.getValue();
                if (creature.isDead || creature.worldObj != player.worldObj) {
                    if (state.sent) CHANNEL.sendTo(packet(creature, state, CemSignalPacket.REMOVE), player);
                    iterator.remove();
                    continue;
                }
                boolean aggressive = creature.getAttackTarget() != null
                    || creature instanceof EntityCreature && ((EntityCreature) creature).getEntityToAttack() != null;
                int sequence = attacks.getOrDefault(creature, 0);
                if (!state.sent || state.aggressive != aggressive || state.sequence != sequence) {
                    int kind = state.sent ? CemSignalPacket.UPDATE : CemSignalPacket.SNAPSHOT;
                    state.aggressive = aggressive;
                    state.sequence = sequence;
                    CHANNEL.sendTo(packet(creature, state, kind), player);
                    state.sent = true;
                }
            }
        }
    }

    private static CemSignalPacket packet(EntityLiving creature, Tracking state, int kind) {
        return new CemSignalPacket(
            creature.dimension,
            creature.getEntityId(),
            state.token,
            state.sequence,
            kind,
            state.aggressive);
    }

    private static final class Tracking {

        final long token;
        boolean sent;
        boolean aggressive;
        int sequence;

        Tracking(long token) {
            this.token = token;
        }
    }

    public static final class Hello implements IMessage {

        int version;
        boolean enabled;

        public Hello() {}

        Hello(boolean enabled) {
            this.version = VERSION;
            this.enabled = enabled;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            if (buffer.readableBytes() == 5) {
                version = buffer.readInt();
                enabled = buffer.readBoolean();
            }
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(version);
            buffer.writeBoolean(enabled);
        }
    }

    public static final class ServerHello implements IMessageHandler<Hello, IMessage> {

        @Override
        public IMessage onMessage(Hello message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            if (!INSTANCE.advertised.contains(context.getServerHandler().netManager)) return null;
            INSTANCE.pending.add(() -> {
                if (!INSTANCE.advertised.contains(player.playerNetServerHandler.netManager)) return;
                if (message.version == VERSION && message.enabled)
                    INSTANCE.peers.put(player, CemConfig.serverAnimationSignals);
                else INSTANCE.peers.remove(player);
                CHANNEL.sendTo(
                    new Hello(message.version == VERSION && message.enabled && CemConfig.serverAnimationSignals),
                    player);
            });
            return null;
        }
    }

    public static final class ClientHello implements IMessageHandler<Hello, IMessage> {

        @Override
        public IMessage onMessage(Hello message, MessageContext context) {
            Salamander.proxy.handleCemHello(context.netHandler, message.version == VERSION && message.enabled);
            return null;
        }
    }

    public static final class ClientSignal implements IMessageHandler<CemSignalPacket, IMessage> {

        @Override
        public IMessage onMessage(CemSignalPacket message, MessageContext context) {
            if (message.valid()) Salamander.proxy.handleCemSignal(context.netHandler, message);
            return null;
        }
    }
}
