package com.geckolib.network.packet.blockentity;

import com.geckolib.network.GeckoLibNetwork;
import com.geckolib.network.RawAnimationPacketCodec;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.salamander.Salamander;
import io.netty.buffer.ByteBuf;

/** Stops one stateless animation key for a loaded client tile entity. */
public final class StatelessBlockEntityStopAnimPacket implements IMessage {

    private int protocolVersion;
    private int x;
    private int y;
    private int z;
    private String animation;

    @SuppressWarnings("unused")
    public StatelessBlockEntityStopAnimPacket() {}

    public StatelessBlockEntityStopAnimPacket(int x, int y, int z, String animation) {
        this.protocolVersion = GeckoLibNetwork.PROTOCOL_VERSION;
        this.x = x;
        this.y = y;
        this.z = z;
        this.animation = animation;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.protocolVersion = buffer.readInt();
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
        this.animation = RawAnimationPacketCodec.readAnimationKey(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.protocolVersion);
        buffer.writeInt(this.x);
        buffer.writeInt(this.y);
        buffer.writeInt(this.z);
        RawAnimationPacketCodec.writeAnimationKey(buffer, this.animation);
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int z() {
        return this.z;
    }

    public String animation() {
        return this.animation;
    }

    public boolean isValid() {
        return this.protocolVersion == GeckoLibNetwork.PROTOCOL_VERSION
            && RawAnimationPacketCodec.isValidAnimationKey(this.animation);
    }

    public static final class Handler implements IMessageHandler<StatelessBlockEntityStopAnimPacket, IMessage> {

        @Override
        public IMessage onMessage(StatelessBlockEntityStopAnimPacket message, MessageContext context) {
            if (message.isValid()) {
                Salamander.proxy.handleStatelessBlockEntityAnimationStop(
                    message.x(),
                    message.y(),
                    message.z(),
                    message.animation());
            }

            return null;
        }
    }
}
