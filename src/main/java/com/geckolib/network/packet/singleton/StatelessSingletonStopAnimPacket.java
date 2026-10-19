package com.geckolib.network.packet.singleton;

import com.geckolib.network.GeckoLibNetwork;
import com.geckolib.network.RawAnimationPacketCodec;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.salamander.Salamander;
import io.netty.buffer.ByteBuf;

/** Stops one stateless animation key for a singleton animatable instance. */
public final class StatelessSingletonStopAnimPacket implements IMessage {

    private static final int MAX_ID_LENGTH = 512;
    private int protocolVersion;
    private String syncableId;
    private long instanceId;
    private String animation;

    @SuppressWarnings("unused")
    public StatelessSingletonStopAnimPacket() {}

    public StatelessSingletonStopAnimPacket(String syncableId, long instanceId, String animation) {
        this.protocolVersion = GeckoLibNetwork.PROTOCOL_VERSION;
        this.syncableId = syncableId;
        this.instanceId = instanceId;
        this.animation = animation;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.protocolVersion = buffer.readInt();
        this.syncableId = ByteBufUtils.readUTF8String(buffer);
        this.instanceId = buffer.readLong();
        this.animation = RawAnimationPacketCodec.readAnimationKey(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.protocolVersion);
        ByteBufUtils.writeUTF8String(buffer, this.syncableId);
        buffer.writeLong(this.instanceId);
        RawAnimationPacketCodec.writeAnimationKey(buffer, this.animation);
    }

    public String syncableId() {
        return this.syncableId;
    }

    public long instanceId() {
        return this.instanceId;
    }

    public String animation() {
        return this.animation;
    }

    public boolean isValid() {
        return this.protocolVersion == GeckoLibNetwork.PROTOCOL_VERSION && isValidId(this.syncableId)
            && this.instanceId != 0
            && RawAnimationPacketCodec.isValidAnimationKey(this.animation);
    }

    private static boolean isValidId(String id) {
        return id != null && !id.isEmpty() && id.length() <= MAX_ID_LENGTH;
    }

    public static final class Handler implements IMessageHandler<StatelessSingletonStopAnimPacket, IMessage> {

        @Override
        public IMessage onMessage(StatelessSingletonStopAnimPacket message, MessageContext context) {
            if (message.isValid()) {
                Salamander.proxy.handleStatelessSingletonAnimationStop(
                    message.syncableId(),
                    message.instanceId(),
                    message.animation());
            }

            return null;
        }
    }
}
