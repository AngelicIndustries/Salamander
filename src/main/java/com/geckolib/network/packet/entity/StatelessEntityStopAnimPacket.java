package com.geckolib.network.packet.entity;

import com.geckolib.network.GeckoLibNetwork;
import com.geckolib.network.RawAnimationPacketCodec;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.salamander.Salamander;
import io.netty.buffer.ByteBuf;

/** Stops one stateless animation key for an ordinary or replaced client entity. */
public final class StatelessEntityStopAnimPacket implements IMessage {

    private int protocolVersion;
    private int entityId;
    private boolean replacedEntity;
    private String animation;

    @SuppressWarnings("unused")
    public StatelessEntityStopAnimPacket() {}

    public StatelessEntityStopAnimPacket(int entityId, boolean replacedEntity, String animation) {
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
        this.animation = RawAnimationPacketCodec.readAnimationKey(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.protocolVersion);
        buffer.writeInt(this.entityId);
        buffer.writeBoolean(this.replacedEntity);
        RawAnimationPacketCodec.writeAnimationKey(buffer, this.animation);
    }

    public int entityId() {
        return this.entityId;
    }

    public boolean isReplacedEntity() {
        return this.replacedEntity;
    }

    public String animation() {
        return this.animation;
    }

    public boolean isValid() {
        return this.protocolVersion == GeckoLibNetwork.PROTOCOL_VERSION && this.entityId >= 0
            && RawAnimationPacketCodec.isValidAnimationKey(this.animation);
    }

    public static final class Handler implements IMessageHandler<StatelessEntityStopAnimPacket, IMessage> {

        @Override
        public IMessage onMessage(StatelessEntityStopAnimPacket message, MessageContext context) {
            if (message.isValid()) {
                Salamander.proxy.handleStatelessEntityAnimationStop(
                    message.entityId(),
                    message.isReplacedEntity(),
                    message.animation());
            }

            return null;
        }
    }
}
