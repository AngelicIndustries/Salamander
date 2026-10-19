package com.geckolib.network.packet.entity;

import com.geckolib.network.GeckoLibNetwork;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.salamander.Salamander;
import io.netty.buffer.ByteBuf;

/** Starts one registered triggerable animation on a client-side entity. */
public final class EntityAnimTriggerPacket implements IMessage {

    private static final int MAX_NAME_LENGTH = 256;

    private int protocolVersion;
    private int entityId;
    private boolean replacedEntity;
    private String controllerName;
    private String animationName;

    @SuppressWarnings("unused")
    public EntityAnimTriggerPacket() {}

    public EntityAnimTriggerPacket(int entityId, String controllerName, String animationName) {
        this(entityId, false, controllerName, animationName);
    }

    public EntityAnimTriggerPacket(int entityId, boolean replacedEntity, String controllerName, String animationName) {
        this.protocolVersion = GeckoLibNetwork.PROTOCOL_VERSION;
        this.entityId = entityId;
        this.replacedEntity = replacedEntity;
        this.controllerName = controllerName;
        this.animationName = animationName;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.protocolVersion = buffer.readInt();
        this.entityId = buffer.readInt();
        this.replacedEntity = buffer.readBoolean();
        this.controllerName = readNullableString(buffer);
        this.animationName = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.protocolVersion);
        buffer.writeInt(this.entityId);
        buffer.writeBoolean(this.replacedEntity);
        writeNullableString(buffer, this.controllerName);
        ByteBufUtils.writeUTF8String(buffer, this.animationName);
    }

    public int entityId() {
        return this.entityId;
    }

    public boolean isReplacedEntity() {
        return this.replacedEntity;
    }

    public String controllerName() {
        return this.controllerName;
    }

    public String animationName() {
        return this.animationName;
    }

    public boolean isValid() {
        return this.protocolVersion == GeckoLibNetwork.PROTOCOL_VERSION && this.entityId >= 0
            && isValidNullableName(this.controllerName)
            && isValidRequiredName(this.animationName);
    }

    private static String readNullableString(ByteBuf buffer) {
        return buffer.readBoolean() ? ByteBufUtils.readUTF8String(buffer) : null;
    }

    private static void writeNullableString(ByteBuf buffer, String value) {
        buffer.writeBoolean(value != null);

        if (value != null) ByteBufUtils.writeUTF8String(buffer, value);
    }

    private static boolean isValidNullableName(String name) {
        return name == null || isValidRequiredName(name);
    }

    private static boolean isValidRequiredName(String name) {
        return name != null && !name.isEmpty() && name.length() <= MAX_NAME_LENGTH;
    }

    public static final class Handler implements IMessageHandler<EntityAnimTriggerPacket, IMessage> {

        @Override
        public IMessage onMessage(EntityAnimTriggerPacket message, MessageContext context) {
            if (message.isValid()) {
                Salamander.proxy.handleEntityAnimationTrigger(
                    message.entityId(),
                    message.isReplacedEntity(),
                    message.controllerName(),
                    message.animationName());
            }

            return null;
        }
    }
}
