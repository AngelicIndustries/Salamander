package com.geckolib.network.packet.entity;

import com.geckolib.animation.RawAnimation;
import com.geckolib.network.GeckoLibNetwork;
import com.geckolib.network.RawAnimationPacketCodec;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.salamander.Salamander;
import io.netty.buffer.ByteBuf;

/** Plays a stateless animation chain for one ordinary or replaced client entity. */
public final class StatelessEntityPlayAnimPacket implements IMessage {

    private int protocolVersion;
    private int entityId;
    private boolean replacedEntity;
    private RawAnimation animation;

    @SuppressWarnings("unused")
    public StatelessEntityPlayAnimPacket() {}

    public StatelessEntityPlayAnimPacket(int entityId, boolean replacedEntity, RawAnimation animation) {
        this.protocolVersion = GeckoLibNetwork.PROTOCOL_VERSION;
        this.entityId = entityId;
        this.replacedEntity = replacedEntity;
        this.animation = animation;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.protocolVersion = buffer.readInt();
        this.entityId = buffer.readInt();
        this.replacedEntity = buffer.readBoolean();
        this.animation = RawAnimationPacketCodec.read(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.protocolVersion);
        buffer.writeInt(this.entityId);
        buffer.writeBoolean(this.replacedEntity);
        RawAnimationPacketCodec.write(buffer, this.animation);
    }

    public int entityId() {
        return this.entityId;
    }

    public boolean isReplacedEntity() {
        return this.replacedEntity;
    }

    public RawAnimation animation() {
        return this.animation;
    }

    public boolean isValid() {
        return this.protocolVersion == GeckoLibNetwork.PROTOCOL_VERSION && this.entityId >= 0
            && RawAnimationPacketCodec.isValid(this.animation);
    }

    public static final class Handler implements IMessageHandler<StatelessEntityPlayAnimPacket, IMessage> {

        @Override
        public IMessage onMessage(StatelessEntityPlayAnimPacket message, MessageContext context) {
            if (message.isValid()) {
                Salamander.proxy.handleStatelessEntityAnimationPlay(
                    message.entityId(),
                    message.isReplacedEntity(),
                    message.animation());
            }

            return null;
        }
    }
}
