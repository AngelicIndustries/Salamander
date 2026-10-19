package com.geckolib.network.packet.blockentity;

import com.geckolib.animation.RawAnimation;
import com.geckolib.network.GeckoLibNetwork;
import com.geckolib.network.RawAnimationPacketCodec;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.salamander.Salamander;
import io.netty.buffer.ByteBuf;

/** Plays a stateless animation chain for one loaded client tile entity. */
public final class StatelessBlockEntityPlayAnimPacket implements IMessage {

    private int protocolVersion;
    private int x;
    private int y;
    private int z;
    private RawAnimation animation;

    @SuppressWarnings("unused")
    public StatelessBlockEntityPlayAnimPacket() {}

    public StatelessBlockEntityPlayAnimPacket(int x, int y, int z, RawAnimation animation) {
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
        this.animation = RawAnimationPacketCodec.read(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.protocolVersion);
        buffer.writeInt(this.x);
        buffer.writeInt(this.y);
        buffer.writeInt(this.z);
        RawAnimationPacketCodec.write(buffer, this.animation);
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

    public RawAnimation animation() {
        return this.animation;
    }

    public boolean isValid() {
        return this.protocolVersion == GeckoLibNetwork.PROTOCOL_VERSION
            && RawAnimationPacketCodec.isValid(this.animation);
    }

    public static final class Handler implements IMessageHandler<StatelessBlockEntityPlayAnimPacket, IMessage> {

        @Override
        public IMessage onMessage(StatelessBlockEntityPlayAnimPacket message, MessageContext context) {
            if (message.isValid()) {
                Salamander.proxy.handleStatelessBlockEntityAnimationPlay(
                    message.x(),
                    message.y(),
                    message.z(),
                    message.animation());
            }

            return null;
        }
    }
}
