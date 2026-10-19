package com.geckolib.network.packet.entity;

import com.geckolib.network.GeckoLibNetwork;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.salamander.Salamander;
import io.netty.buffer.ByteBuf;

/** Synchronizes one Citadel animation selection to clients tracking an entity. */
public final class CitadelAnimationPacket implements IMessage {

    private int protocolVersion;
    private int entityId;
    private int animationIndex;

    @SuppressWarnings("unused")
    public CitadelAnimationPacket() {}

    public CitadelAnimationPacket(int entityId, int animationIndex) {
        this.protocolVersion = GeckoLibNetwork.PROTOCOL_VERSION;
        this.entityId = entityId;
        this.animationIndex = animationIndex;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.protocolVersion = buffer.readInt();
        this.entityId = buffer.readInt();
        this.animationIndex = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.protocolVersion);
        buffer.writeInt(this.entityId);
        buffer.writeInt(this.animationIndex);
    }

    public int entityId() {
        return this.entityId;
    }

    public int animationIndex() {
        return this.animationIndex;
    }

    public boolean isValid() {
        return this.protocolVersion == GeckoLibNetwork.PROTOCOL_VERSION && this.entityId >= 0
            && this.animationIndex >= -1;
    }

    public static final class Handler implements IMessageHandler<CitadelAnimationPacket, IMessage> {

        @Override
        public IMessage onMessage(CitadelAnimationPacket message, MessageContext context) {
            if (message.isValid()) {
                Salamander.proxy.handleCitadelAnimation(message.entityId(), message.animationIndex());
            }

            return null;
        }
    }
}
