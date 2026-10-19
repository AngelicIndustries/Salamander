package industries.angelic.salamander.cem.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import industries.angelic.salamander.cem.network.CemNetwork;
import industries.angelic.salamander.cem.network.CemSignalPacket;
import industries.angelic.salamander.cem.network.CemSignalState;

/** Client-only transport state. Entity object keys isolate unloads and entity-ID reuse. */
public final class CemClientSignals {

    public static final CemClientSignals INSTANCE = new CemClientSignals();
    private static final Map<Entity, CemSignalState> SIGNALS = new WeakHashMap<>();
    private static final Map<Integer, Pending> PENDING = new HashMap<>();
    private static Object advertised;
    private static Object requested;
    private static boolean enabled;
    private static World world;
    private static long tick;

    private CemClientSignals() {}

    public static void registration(Object manager, boolean registered) {
        if (registered) advertised = manager;
        else if (advertised == manager) reset();
    }

    public static void hello(Object handler, boolean accepted) {
        if (Minecraft.getMinecraft()
            .getNetHandler() != handler) return;
        enabled = accepted;
        if (!accepted) {
            SIGNALS.clear();
            PENDING.clear();
        }
    }

    public static void receive(Object handler, CemSignalPacket packet) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!enabled || minecraft.getNetHandler() != handler
            || minecraft.theWorld == null
            || minecraft.theWorld.provider.dimensionId != packet.dimension) return;
        updateWorld(minecraft.theWorld);
        Entity entity = minecraft.theWorld.getEntityByID(packet.entityId);
        if (packet.kind == CemSignalPacket.REMOVE) {
            Pending pending = PENDING.get(packet.entityId);
            if (pending != null && pending.packet.token == packet.token) PENDING.remove(packet.entityId);
            CemSignalState signal = SIGNALS.get(entity);
            if (signal != null && signal.token == packet.token) SIGNALS.remove(entity);
            return;
        }
        if (entity == null) {
            Pending pending = PENDING.get(packet.entityId);
            if (packet.kind == CemSignalPacket.SNAPSHOT) {
                if ((pending == null && PENDING.size() < 1024)
                    || (pending != null && packet.token > pending.packet.token))
                    PENDING.put(packet.entityId, new Pending(packet, tick + 40));
            } else if (pending != null && packet.token == pending.packet.token
                && packet.sequence - pending.packet.sequence >= 0) {
                    // Keep the initial snapshot while the vanilla spawn packet is still being processed.
                    // Attacks before an entity exists must not replay after it appears.
                    PENDING.put(
                        packet.entityId,
                        new Pending(
                            new CemSignalPacket(
                                packet.dimension,
                                packet.entityId,
                                packet.token,
                                packet.sequence,
                                CemSignalPacket.SNAPSHOT,
                                packet.aggressive),
                            pending.expires));
                }
        } else apply(entity, packet);
    }

    private static void apply(Entity entity, CemSignalPacket packet) {
        if (!CemNetwork.supported(entity)) return;
        CemSignalState state = SIGNALS.get(entity);
        if (packet.kind == CemSignalPacket.SNAPSHOT) {
            if (state == null || packet.token > state.token) SIGNALS.put(entity, new CemSignalState(packet));
        } else if (state != null) state.update(packet, tick);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tick++;
        Minecraft minecraft = Minecraft.getMinecraft();
        updateWorld(minecraft.theWorld);
        if (minecraft.getNetHandler() != null && minecraft.theWorld != null
            && minecraft.getNetHandler()
                .getNetworkManager() == advertised
            && requested != advertised) {
            requested = advertised;
            CemNetwork.request();
        }
        if (minecraft.theWorld == null) return;
        Iterator<Map.Entry<Integer, Pending>> iterator = PENDING.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Pending> entry = iterator.next();
            Entity entity = minecraft.theWorld.getEntityByID(entry.getKey());
            if (entry.getValue().expires < tick
                || entry.getValue().packet.dimension != minecraft.theWorld.provider.dimensionId) iterator.remove();
            else if (entity != null) {
                apply(entity, entry.getValue().packet);
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> { if (advertised == event.manager || requested == event.manager) reset(); });
    }

    public static boolean aggressive(Entity entity) {
        CemSignalState state = enabled ? SIGNALS.get(entity) : null;
        if (state != null) return state.aggressive();
        if (entity instanceof net.minecraft.entity.monster.EntityEnderman)
            return ((net.minecraft.entity.monster.EntityEnderman) entity).isScreaming();
        if (entity instanceof net.minecraft.entity.passive.EntityWolf)
            return ((net.minecraft.entity.passive.EntityWolf) entity).isAngry();
        if (entity instanceof net.minecraft.entity.monster.EntityCreeper)
            return ((net.minecraft.entity.monster.EntityCreeper) entity).getCreeperState() > 0;
        if (entity instanceof net.minecraft.entity.EntityLiving)
            return ((net.minecraft.entity.EntityLiving) entity).getAttackTarget() != null;
        return false;
    }

    public static double swing(EntityLivingBase entity, float partialTicks) {
        CemSignalState state = enabled ? SIGNALS.get(entity) : null;
        double swing = state == null ? -1 : state.swing(tick, partialTicks);
        return swing >= 0 ? swing : entity.getSwingProgress(partialTicks);
    }

    private static void reset() {
        advertised = requested = null;
        enabled = false;
        world = null;
        SIGNALS.clear();
        PENDING.clear();
    }

    private static void updateWorld(World current) {
        if (world != current) {
            SIGNALS.clear();
            PENDING.clear();
            world = current;
        }
    }

    private static final class Pending {

        final CemSignalPacket packet;
        final long expires;

        Pending(CemSignalPacket packet, long expires) {
            this.packet = packet;
            this.expires = expires;
        }
    }
}
