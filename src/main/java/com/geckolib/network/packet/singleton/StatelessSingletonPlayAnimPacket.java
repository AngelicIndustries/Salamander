package com.geckolib.network.packet.singleton;

import com.geckolib.animation.RawAnimation;
import com.geckolib.network.GeckoLibNetwork;
import com.geckolib.network.RawAnimationPacketCodec;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.salamander.Salamander;
import io.netty.buffer.ByteBuf;

/** Plays a stateless animation chain for one singleton animatable instance. */
public final class StatelessSingletonPlayAnimPacket implements IMessage {

    private static final int MAX_ID_LENGTH = 512;

    private int protocolVersion;
    private String syncableId;
    private long instanceId;
    private RawAnimation animation;

    @SuppressWarnings("unused")
    public StatelessSingletonPlayAnimPacket() {}

    public StatelessSingletonPlayAnimPacket(String syncableId, long instanceId, RawAnimation animation) {
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
        this.animation = RawAnimationPacketCodec.read(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.protocolVersion);
        ByteBufUtils.writeUTF8String(buffer, this.syncableId);
        buffer.writeLong(this.instanceId);
        RawAnimationPacketCodec.write(buffer, this.animation);
    }

    public String syncableId() {
        return this.syncableId;
    }

    public long instanceId() {
        return this.instanceId;
    }

    public RawAnimation animation() {
        return this.animation;
    }

    public boolean isValid() {
        return this.protocolVersion == GeckoLibNetwork.PROTOCOL_VERSION && isValidId(this.syncableId)
            && this.instanceId != 0
            && RawAnimationPacketCodec.isValid(this.animation);
    }

    private static boolean isValidId(String id) {
        return id != null && !id.isEmpty() && id.length() <= MAX_ID_LENGTH;
    }

    public static final class Handler implements IMessageHandler<StatelessSingletonPlayAnimPacket, IMessage> {

        @Override
        public IMessage onMessage(StatelessSingletonPlayAnimPacket message, MessageContext context) {
            if (message.isValid()) {
                Salamander.proxy.handleStatelessSingletonAnimationPlay(
                    message.syncableId(),
                    message.instanceId(),
                    message.animation());
            }

            return null;
        }
    }
}
